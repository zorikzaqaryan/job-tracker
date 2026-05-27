package com.zak.jobhunter.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Structured JSON response from the AI provider, mapped 1-to-1 from the model output.
 *
 * <p>Allowed {@code decision} values:
 * QUALIFIED, NOT_QUALIFIED, NEEDS_REVIEW,
 * REJECTED_LOCATION, REJECTED_SENIORITY, REJECTED_TECH_STACK, REJECTED_REMOTE_POLICY
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JobAiAnalysisResult(
        boolean relevant,
        int qualificationScore,
        boolean remoteCompatible,
        boolean locationCompatible,
        boolean seniorityCompatible,
        List<String> matchedSkills,
        List<String> missingImportantSkills,
        List<String> riskFlags,
        String decision,
        String reason
) {
    public static JobAiAnalysisResult error(String reason) {
        return new JobAiAnalysisResult(
                false, 0, false, false, false,
                List.of(), List.of(), List.of("ANALYSIS_ERROR"),
                "NOT_QUALIFIED", reason);
    }
}
