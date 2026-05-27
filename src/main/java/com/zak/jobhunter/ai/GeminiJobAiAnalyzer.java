package com.zak.jobhunter.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zak.jobhunter.ai.dto.CandidateContextDto;
import com.zak.jobhunter.ai.dto.JobAiAnalysisResult;
import com.zak.jobhunter.ai.dto.JobContextDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Google Gemini implementation of {@link JobAiAnalyzer}.
 *
 * <p>Active when {@code app.ai.provider=gemini} (case-insensitive).
 * Requires {@code GEMINI_API_KEY} to be set.
 *
 * <p>Uses Spring AI {@link ChatClient} to call the model and expects a JSON response
 * (enforced via {@code response-mime-type: application/json} in {@code application.yml}).
 */
@Component("geminiJobAiAnalyzer")
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "gemini", matchIfMissing = false)
public class GeminiJobAiAnalyzer implements JobAiAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(GeminiJobAiAnalyzer.class);

    private final ChatClient     chatClient;
    private final AiPromptBuilder promptBuilder;
    private final AiProperties   aiProperties;
    private final ObjectMapper   objectMapper;

    public GeminiJobAiAnalyzer(ChatModel chatModel,
                                AiPromptBuilder promptBuilder,
                                AiProperties aiProperties,
                                ObjectMapper objectMapper) {
        this.chatClient    = ChatClient.builder(chatModel).build();
        this.promptBuilder = promptBuilder;
        this.aiProperties  = aiProperties;
        this.objectMapper  = objectMapper;
    }

    @Override
    public JobAiAnalysisResult analyze(JobContextDto job, CandidateContextDto candidate) {
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt   = promptBuilder.buildUserPrompt(job, candidate);

        // Truncate description if needed to stay within token budget
        if (userPrompt.length() > aiProperties.maxDescriptionChars()) {
            userPrompt = userPrompt.substring(0, aiProperties.maxDescriptionChars());
        }

        log.info("[GEMINI] Calling Gemini for jobId={} model configured via spring.ai.google.genai",
                job.jobId());

        try {
            String rawJson = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            log.debug("[GEMINI] Raw response for jobId={}: {}", job.jobId(), rawJson);

            JobAiAnalysisResult result = objectMapper.readValue(sanitizeJson(rawJson),
                    JobAiAnalysisResult.class);

            // Validate score range
            if (result.qualificationScore() < 0 || result.qualificationScore() > 100) {
                throw new AiAnalysisException("AI returned out-of-range score: " + result.qualificationScore());
            }

            return result;

        } catch (AiAnalysisException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[GEMINI] API call failed for jobId={}: {}", job.jobId(), ex.getMessage(), ex);
            throw new AiAnalysisException("Gemini API call failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public AiProvider provider() {
        return AiProvider.GEMINI;
    }

    /**
     * Strip markdown code fences that models sometimes add despite instructions.
     * e.g. ```json { ... } ```  →  { ... }
     */
    private String sanitizeJson(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.strip();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed;
    }
}
