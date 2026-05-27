package com.zak.jobhunter.channel.dto;

import com.zak.jobhunter.channel.SourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body to update an existing job source channel")
public record UpdateChannelRequest(
        @NotNull SourceType sourceType,
        @NotBlank String name,
        String telegramUsername,
        String telegramChannelId,
        String url,
        Boolean enabled
) {}
