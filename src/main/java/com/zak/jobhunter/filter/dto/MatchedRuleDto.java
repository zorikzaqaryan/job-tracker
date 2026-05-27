package com.zak.jobhunter.filter.dto;

import com.zak.jobhunter.filter.RuleField;

public record MatchedRuleDto(
        Long ruleId,
        String keyword,
        RuleField field,
        String matchedField,
        String matchedText,
        int weight
) {}
