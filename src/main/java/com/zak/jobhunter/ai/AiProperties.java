package com.zak.jobhunter.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * AI pipeline configuration bound to {@code app.ai.*}.
 */
@Validated
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        boolean enabled,
        String provider,
        int minRuleScoreBeforeAi,
        int minQualificationScore,
        int maxDescriptionChars,
        boolean requireUrlEnrichment,
        boolean requireHumanApproval,
        boolean storeRawPrompts,
        boolean sendCvToAi,
        RetryConfig retry
) {
    public record RetryConfig(
            int maxAttempts,
            int initialDelaySeconds,
            double multiplier
    ) {}

    /** Resolved provider enum; defaults to MOCK if unknown. */
    public AiProvider resolvedProvider() {
        try {
            return AiProvider.valueOf(provider.toUpperCase());
        } catch (Exception e) {
            return AiProvider.MOCK;
        }
    }
}
