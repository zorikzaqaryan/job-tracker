package com.zak.jobhunter.filter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ad-hoc matching test payload")
public record MatchTestRequest(
        @Schema(example = "Senior Java Backend Developer") String title,
        @Schema(example = "Remote role, open to Armenia and worldwide candidates.") String description,
        @Schema(example = "Yerevan / Remote") String location
) {}
