package com.zak.jobhunter.enrichment.dto;

public record UrlEnrichmentRequest(
        Long jobId,
        String url
) {}
