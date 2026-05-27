package com.zak.jobhunter.ai;

import com.zak.jobhunter.ai.dto.JobAiAnalysisResult;
import com.zak.jobhunter.job.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Deterministic decision engine that converts an AI result into a {@link JobStatus}.
 *
 * <p>The AI recommendation is treated as a signal, not a command.
 * Business rules here always take precedence over the AI decision string.
 */
@Service
public class AiDecisionService {

    private static final Logger log = LoggerFactory.getLogger(AiDecisionService.class);

    /** Risk flags that automatically reject a job regardless of AI score. */
    private static final Set<String> CRITICAL_FLAGS = Set.of(
            "US_ONLY",
            "ONSITE_ONLY",
            "REQUIRES_LOCAL_WORK_AUTHORIZATION",
            "INTERNSHIP",
            "UNPAID",
            "JUNIOR_ONLY",
            "NOT_REMOTE"
    );

    /**
     * Decide the final job status from the AI analysis result and configured thresholds.
     *
     * @param result             structured AI output
     * @param aiProperties       threshold configuration
     * @return {@link JobStatus#AI_MATCHED} or {@link JobStatus#AI_REJECTED}
     */
    public JobStatus decide(JobAiAnalysisResult result, AiProperties aiProperties) {

        // 1. Not relevant at all
        if (!result.relevant()) {
            log.debug("[AI-DECISION] Rejected — relevant=false reason='{}'", result.reason());
            return JobStatus.AI_REJECTED;
        }

        // 2. Score below threshold
        if (result.qualificationScore() < aiProperties.minQualificationScore()) {
            log.debug("[AI-DECISION] Rejected — score={} < threshold={}",
                    result.qualificationScore(), aiProperties.minQualificationScore());
            return JobStatus.AI_REJECTED;
        }

        // 3. Remote incompatibility
        if (!result.remoteCompatible()) {
            log.debug("[AI-DECISION] Rejected — remoteCompatible=false");
            return JobStatus.AI_REJECTED;
        }

        // 4. Location incompatibility
        if (!result.locationCompatible()) {
            log.debug("[AI-DECISION] Rejected — locationCompatible=false");
            return JobStatus.AI_REJECTED;
        }

        // 5. Critical risk flags
        if (result.riskFlags() != null) {
            for (String flag : result.riskFlags()) {
                if (CRITICAL_FLAGS.contains(flag.toUpperCase())) {
                    log.debug("[AI-DECISION] Rejected — critical risk flag: {}", flag);
                    return JobStatus.AI_REJECTED;
                }
            }
        }

        // 6. Human approval gate (still marks AI_MATCHED; human acts on it separately)
        log.info("[AI-DECISION] ✔ MATCHED — score={} decision='{}' flags={}",
                result.qualificationScore(), result.decision(), result.riskFlags());
        return JobStatus.AI_MATCHED;
    }
}
