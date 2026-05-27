package com.zak.jobhunter.channel.dto;

import com.zak.jobhunter.channel.SourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body to create a new job source channel")
public record CreateChannelRequest(
        @NotNull @Schema(description = "Channel source type", example = "TELEGRAM")
        SourceType sourceType,

        @NotBlank @Schema(description = "Human-readable name", example = "Java Jobs Armenia")
        String name,

        @Schema(description = "Telegram @username (without @)", example = "java_jobs_armenia")
        String telegramUsername,

        @Schema(description = "Telegram channel ID, e.g. -100123456789", example = "-100123456789")
        String telegramChannelId,

        @Schema(description = "Source URL for website-type sources")
        String url,

        @Schema(description = "Whether this source is active", defaultValue = "true")
        Boolean enabled
) {}
