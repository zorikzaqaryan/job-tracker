package com.zak.jobhunter.ingestion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(description = "Inbound raw message payload from any job source")
public record RawMessageDto(
        @NotBlank @Schema(example = "TELEGRAM") String sourceType,
        @Schema(example = "Java Jobs Armenia") String sourceName,
        @NotBlank @Schema(example = "-100123456789") String sourceChannelId,
        @NotNull  @Schema(example = "98765") String externalMessageId,
        @NotBlank @Schema(example = "Senior Java Developer. Remote. Armenia or Worldwide.") String text,
        @Schema(example = "https://t.me/java_jobs_armenia/98765") String url,
        @Schema(example = "2026-05-26T12:30:00Z") Instant publishedAt,
        @Schema(description = "Optional: resolved DB source ID") Long sourceId
) {}
