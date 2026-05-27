package com.zak.jobhunter.ai.dto;

import java.util.List;

/**
 * Job context assembled for the AI prompt.
 */
public record JobContextDto(
        Long jobId,
        String title,
        String company,
        String location,
        String description,
        String url,
        String enrichedText,
        int ruleScore,
        List<String> matchedKeywords
) {}
