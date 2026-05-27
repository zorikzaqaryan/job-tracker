package com.zak.jobhunter.ai.dto;

import java.util.List;

/**
 * Message published to the {@code ai-job-analysis-requests} RabbitMQ queue.
 */
public record AiJobAnalysisRequest(
        Long jobId,
        String title,
        String company,
        String location,
        String description,
        String url,
        int ruleScore,
        List<String> matchedKeywords
) {}
