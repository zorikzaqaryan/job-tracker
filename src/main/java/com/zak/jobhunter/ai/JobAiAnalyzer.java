package com.zak.jobhunter.ai;

import com.zak.jobhunter.ai.dto.CandidateContextDto;
import com.zak.jobhunter.ai.dto.JobAiAnalysisResult;
import com.zak.jobhunter.ai.dto.JobContextDto;

/**
 * Strategy interface for AI-based job qualification analysis.
 *
 * <p>Implementations must be deterministic and stateless. They receive structured
 * context about the candidate and the job, and return a {@link JobAiAnalysisResult}.
 *
 * <p>Current implementations: {@link GeminiJobAiAnalyzer}, {@link MockJobAiAnalyzer}.
 * Future: Ollama, OpenRouter, DeepSeek, NVIDIA.
 */
public interface JobAiAnalyzer {

    /**
     * Analyze whether the job is a good match for the candidate.
     *
     * @param job       structured job context (title, description, enriched text, etc.)
     * @param candidate candidate profile context (skills, preferences, etc.)
     * @return analysis result with score, flags, and decision
     * @throws AiAnalysisException if the provider call fails and should not be retried
     */
    JobAiAnalysisResult analyze(JobContextDto job, CandidateContextDto candidate);

    /** Provider identifier for logging and audit records. */
    AiProvider provider();

    class AiAnalysisException extends RuntimeException {
        public AiAnalysisException(String message, Throwable cause) {
            super(message, cause);
        }
        public AiAnalysisException(String message) {
            super(message);
        }
    }
}
