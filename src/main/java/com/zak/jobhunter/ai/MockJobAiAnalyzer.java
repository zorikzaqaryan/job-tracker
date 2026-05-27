package com.zak.jobhunter.ai;

import com.zak.jobhunter.ai.dto.CandidateContextDto;
import com.zak.jobhunter.ai.dto.JobAiAnalysisResult;
import com.zak.jobhunter.ai.dto.JobContextDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic mock implementation for local development and testing.
 *
 * <p>Rules:
 * <ul>
 *   <li>Job title/description contains "Java" AND ("Remote" or "Worldwide") → QUALIFIED (score 88)</li>
 *   <li>Job contains "US only" or "onsite only" → REJECTED_LOCATION</li>
 *   <li>Job is internship or unpaid → REJECTED (score 10)</li>
 *   <li>Otherwise → NEEDS_REVIEW (score 55)</li>
 * </ul>
 *
 * Active when no other {@link JobAiAnalyzer} bean is registered (e.g. when
 * {@code app.ai.provider=mock} or when the Gemini bean is not available).
 */
@Component
@ConditionalOnMissingBean(name = "geminiJobAiAnalyzer")
public class MockJobAiAnalyzer implements JobAiAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(MockJobAiAnalyzer.class);

    @Override
    public JobAiAnalysisResult analyze(JobContextDto job, CandidateContextDto candidate) {
        log.info("[MOCK-AI] Analyzing job '{}' (mock provider — no real API call)", job.title());

        String combined = lower(job.title()) + " " + lower(job.description()) + " " + lower(job.location());

        if (combined.contains("us only") || combined.contains("onsite only") || combined.contains("only us")) {
            return new JobAiAnalysisResult(
                    false, 5, false, false, true,
                    List.of(), List.of(),
                    List.of("US_ONLY"),
                    "REJECTED_LOCATION",
                    "[MOCK] Rejected: US-only or onsite-only role.");
        }

        if (combined.contains("internship") || combined.contains("unpaid")) {
            return new JobAiAnalysisResult(
                    false, 10, false, false, false,
                    List.of(), List.of(),
                    List.of("INTERNSHIP"),
                    "NOT_QUALIFIED",
                    "[MOCK] Rejected: internship or unpaid role.");
        }

        boolean hasJava   = combined.contains("java");
        boolean hasRemote = combined.contains("remote") || combined.contains("worldwide");

        if (hasJava && hasRemote) {
            List<String> skills = buildMatchedSkills(combined);
            return new JobAiAnalysisResult(
                    true, 88, true, true, true,
                    skills, List.of(),
                    List.of(),
                    "QUALIFIED",
                    "[MOCK] Strong Java + remote match.");
        }

        if (hasJava) {
            return new JobAiAnalysisResult(
                    true, 60, false, false, true,
                    List.of("Java"), List.of(),
                    List.of("REMOTE_POLICY_UNCLEAR"),
                    "NEEDS_REVIEW",
                    "[MOCK] Java match but remote policy unclear.");
        }

        return new JobAiAnalysisResult(
                false, 30, false, false, false,
                List.of(), List.of(),
                List.of("TECH_STACK_MISMATCH"),
                "NOT_QUALIFIED",
                "[MOCK] No relevant Java/backend signals found.");
    }

    @Override
    public AiProvider provider() {
        return AiProvider.MOCK;
    }

    private String lower(String s) {
        return s != null ? s.toLowerCase() : "";
    }

    private List<String> buildMatchedSkills(String text) {
        List<String> skills = new ArrayList<>();
        for (String skill : List.of("java", "spring boot", "spring", "kafka", "rabbitmq",
                "postgresql", "kubernetes", "docker", "aws", "gcp")) {
            if (text.contains(skill)) skills.add(skill);
        }
        return skills;
    }
}
