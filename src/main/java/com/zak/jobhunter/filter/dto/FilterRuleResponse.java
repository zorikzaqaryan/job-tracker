package com.zak.jobhunter.filter.dto;

import com.zak.jobhunter.filter.MatchType;
import com.zak.jobhunter.filter.RuleField;
import com.zak.jobhunter.filter.RuleType;

import java.time.Instant;

public record FilterRuleResponse(
        Long id,
        String keyword,
        String normalizedKeyword,
        MatchType matchType,
        RuleField field,
        RuleType ruleType,
        int weight,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {}
