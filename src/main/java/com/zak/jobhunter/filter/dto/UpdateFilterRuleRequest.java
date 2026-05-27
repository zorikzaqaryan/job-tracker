package com.zak.jobhunter.filter.dto;

import com.zak.jobhunter.filter.MatchType;
import com.zak.jobhunter.filter.RuleField;
import com.zak.jobhunter.filter.RuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateFilterRuleRequest(
        @NotBlank String keyword,
        @NotNull  MatchType matchType,
        @NotNull  RuleField field,
        @NotNull  RuleType ruleType,
        int weight,
        Boolean enabled
) {}
