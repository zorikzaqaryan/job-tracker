package com.zak.jobhunter.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zak.jobhunter.ai.dto.AiJobAnalysisRequest;
import com.zak.jobhunter.ai.dto.CandidateContextDto;
import com.zak.jobhunter.ai.dto.JobAiAnalysisResult;
import com.zak.jobhunter.ai.dto.JobContextDto;
import com.zak.jobhunter.candidate.CandidateProfile;
import com.zak.jobhunter.candidate.CandidateService;
import com.zak.jobhunter.enrichment.JobUrlEnrichmentRepository;
import com.zak.jobhunter.enrichment.UrlEnrichmentStatus;
import com.zak.jobhunter.job.JobPost;
import com.zak.jobhunter.job.JobPostRepository;
import com.zak.jobhunter.job.JobStatus;
import com.zak.jobhunter.messaging.RabbitQueues;
import com.zak.jobhunter.telegram.TelegramBotSender;
import com.zak.jobhunter.telegram.TelegramMessageFormatter;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Consumes AI analysis requests from {@code ai-job-analysis-requests}.
 *
 * <p>Pipeline:
 * <ol>
 *   <li>Load job from DB.</li>
 *   <li>Load active candidate profile.</li>
 *   <li>Load URL-enriched text if available.</li>
 *   <li>Build context and call {@link JobAiAnalyzer}.</li>
 *   <li>Persist {@link JobAiAnalysis} record.</li>
 *   <li>Run {@link AiDecisionService} to determine final status.</li>
 *   <li>Update job status.</li>
 *   <li>If AI_MATCHED → send Telegram notification.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class AiAnalysisListener {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisListener.class);

    private final JobPostRepository        jobPostRepository;
    private final JobAiAnalysisRepository  analysisRepository;
    private final JobAiAnalyzer            jobAiAnalyzer;
    private final AiDecisionService        decisionService;
    private final CandidateService         candidateService;
    private final JobUrlEnrichmentRepository enrichmentRepository;
    private final AiProperties             aiProperties;
    private final TelegramBotSender        telegramBotSender;
    private final TelegramMessageFormatter formatter;
    private final ObjectMapper             objectMapper;

    @RabbitListener(queues = RabbitQueues.AI_ANALYSIS_QUEUE,
                    containerFactory = "aiListenerContainerFactory")
    @Transactional
    public void onAiAnalysisRequest(AiJobAnalysisRequest request) {
        log.info("[AI-LISTENER] ▶ Received analysis request  jobId={} ruleScore={}",
                request.jobId(), request.ruleScore());

        JobPost job = jobPostRepository.findById(request.jobId())
                .orElseThrow(() -> new EntityNotFoundException("Job not found: " + request.jobId()));

        // Skip if already processed by a previous attempt
        if (job.getStatus() == JobStatus.AI_MATCHED || job.getStatus() == JobStatus.AI_REJECTED) {
            log.info("[AI-LISTENER] ⏭ Already decided  jobId={} status={}", job.getId(), job.getStatus());
            return;
        }

        // Load candidate profile
        CandidateProfile candidate = candidateService.findMostRecent();
        CandidateContextDto candidateCtx = buildCandidateContext(candidate);

        // Load URL-enriched text
        String enrichedText = enrichmentRepository
                .findFirstByJobIdAndFetchStatus(job.getId(), UrlEnrichmentStatus.SUCCESS.name())
                .map(e -> e.getExtractedText())
                .orElse(null);

        JobContextDto jobCtx = new JobContextDto(
                job.getId(), job.getTitle(), job.getCompany(), job.getLocation(),
                truncate(job.getDescription(), aiProperties.maxDescriptionChars()),
                job.getUrl(), enrichedText,
                request.ruleScore(), request.matchedKeywords()
        );

        // Build raw request JSON for audit
        String rawRequestJson = toJson(jobCtx);
        JobAiAnalysisResult result;
        String rawResponseJson;

        try {
            result         = jobAiAnalyzer.analyze(jobCtx, candidateCtx);
            rawResponseJson = aiProperties.storeRawPrompts() ? toJson(result) : null;
            log.info("[AI-LISTENER] Analysis done  jobId={} score={} decision='{}'",
                    job.getId(), result.qualificationScore(), result.decision());
        } catch (JobAiAnalyzer.AiAnalysisException ex) {
            log.error("[AI-LISTENER] ✘ AI provider failed  jobId={} error={}", job.getId(), ex.getMessage());
            job.setStatus(JobStatus.AI_ANALYSIS_FAILED);
            jobPostRepository.save(job);
            persistAnalysis(job, result = JobAiAnalysisResult.error(ex.getMessage()),
                    rawRequestJson, null, ex.getMessage());
            throw ex; // re-throw → RabbitMQ retry / DLQ
        }

        // Persist analysis record
        persistAnalysis(job, result,
                aiProperties.storeRawPrompts() ? rawRequestJson : null,
                rawResponseJson, null);

        // Determine final status
        JobStatus finalStatus = decisionService.decide(result, aiProperties);
        job.setStatus(finalStatus);
        jobPostRepository.save(job);

        log.info("[AI-LISTENER] Final status  jobId={} status={}", job.getId(), finalStatus);

        if (finalStatus == JobStatus.AI_MATCHED) {
            notifyTelegram(job, result);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private void persistAnalysis(JobPost job, JobAiAnalysisResult result,
                                  String rawRequest, String rawResponse, String error) {
        JobAiAnalysis analysis = JobAiAnalysis.builder()
                .job(job)
                .provider(jobAiAnalyzer.provider().name().toLowerCase())
                .model(resolveModelName())
                .relevant(result.relevant())
                .qualificationScore(result.qualificationScore())
                .remoteCompatible(result.remoteCompatible())
                .locationCompatible(result.locationCompatible())
                .seniorityCompatible(result.seniorityCompatible())
                .matchedSkills(listToString(result.matchedSkills()))
                .missingImportantSkills(listToString(result.missingImportantSkills()))
                .riskFlags(listToString(result.riskFlags()))
                .decision(result.decision() != null ? result.decision() : "UNKNOWN")
                .reason(result.reason())
                .rawRequest(rawRequest)
                .rawResponse(rawResponse)
                .errorMessage(error)
                .build();
        analysisRepository.save(analysis);
    }

    private void notifyTelegram(JobPost job, JobAiAnalysisResult result) {
        try {
            String message = formatter.formatAiMatch(job, result);
            telegramBotSender.sendMessage(message);
            log.info("[AI-LISTENER] ✔ Telegram notification sent  jobId={}", job.getId());
        } catch (Exception ex) {
            log.error("[AI-LISTENER] Telegram send failed  jobId={}: {}", job.getId(), ex.getMessage());
        }
    }

    private CandidateContextDto buildCandidateContext(CandidateProfile profile) {
        if (profile == null) {
            log.warn("[AI-LISTENER] No candidate profile found — using defaults");
            return new CandidateContextDto(
                    "Senior Java Backend Developer", "Yerevan, Armenia", 9,
                    null, "Senior Java Backend Engineer",
                    "Remote, Worldwide, Europe, Armenia",
                    "Remote, Full Remote", "US-only, onsite-only, internship, unpaid",
                    List.of("Java", "Spring Boot", "PostgreSQL", "Kafka", "RabbitMQ",
                            "Docker", "Kubernetes", "AWS", "GCP", "Angular"));
        }
        List<String> skills = profile.getSkills().stream()
                .map(s -> s.getSkillName() + (s.getLevel() != null ? " (" + s.getLevel() + ")" : ""))
                .toList();
        return new CandidateContextDto(
                profile.getCurrentTitle(), profile.getLocation(),
                profile.getYearsOfExperience(), profile.getSummary(),
                profile.getPreferredJobTitles(), profile.getPreferredLocations(),
                profile.getPreferredWorkModes(), profile.getAvoidRules(), skills);
    }

    private String truncate(String text, int maxChars) {
        if (text == null) return "";
        return text.length() > maxChars ? text.substring(0, maxChars) + "…" : text;
    }

    private String listToString(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return list.stream().collect(Collectors.joining(", "));
    }

    private String resolveModelName() {
        return "gemini-2.0-flash"; // matches default in application.yml
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
