package com.zak.jobhunter.channel.dto;

import com.zak.jobhunter.channel.SourceType;

import java.time.Instant;

public record ChannelResponse(
        Long id,
        SourceType sourceType,
        String name,
        String telegramUsername,
        String telegramChannelId,
        String url,
        boolean enabled,
        String lastExternalMessageId,
        Instant createdAt,
        Instant updatedAt
) {}
