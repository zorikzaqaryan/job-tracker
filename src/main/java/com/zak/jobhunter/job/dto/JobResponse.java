package com.zak.jobhunter.job.dto;

import com.zak.jobhunter.job.JobStatus;

import java.time.Instant;
import java.util.List;

public record JobResponse(
        Long id,
        Long rawMessageId,
        String title,
        String company,
        String location,
        String description,
        String url,
        /** Deep link to the original Telegram post — "Go to TG message". */
        String telegramMessageUrl,
        String sourceChannelName,
        int score,
        JobStatus status,
        String contentHash,
        Instant sentAt,
        Instant createdAt,
        Instant updatedAt,
        List<RuleMatchResponse> matchedRules
) {
    public record RuleMatchResponse(Long ruleId, String matchedField, String matchedText, int weight) {}
}
