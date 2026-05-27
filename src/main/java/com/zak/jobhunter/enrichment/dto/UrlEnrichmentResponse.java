package com.zak.jobhunter.enrichment.dto;

import java.time.Instant;

public record UrlEnrichmentResponse(
        Long id,
        Long jobId,
        String url,
        String fetchStatus,
        Integer httpStatus,
        String extractedTitle,
        String extractedDescription,
        String extractedApplyUrl,
        String errorMessage,
        Instant createdAt
) {}
