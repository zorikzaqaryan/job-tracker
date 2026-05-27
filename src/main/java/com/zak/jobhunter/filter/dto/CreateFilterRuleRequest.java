package com.zak.jobhunter.filter.dto;

import com.zak.jobhunter.filter.MatchType;
import com.zak.jobhunter.filter.RuleField;
import com.zak.jobhunter.filter.RuleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body for creating a filter rule")
public record CreateFilterRuleRequest(
        @NotBlank @Schema(example = "Remote") String keyword,
        @NotNull  @Schema(example = "WHOLE_WORD") MatchType matchType,
        @NotNull  @Schema(example = "ANY") RuleField field,
        @NotNull  @Schema(example = "POSITIVE") RuleType ruleType,
        @Schema(example = "5") int weight,
        @Schema(defaultValue = "true") Boolean enabled
) {}
