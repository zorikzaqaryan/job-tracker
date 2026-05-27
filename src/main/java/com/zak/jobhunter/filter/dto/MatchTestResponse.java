package com.zak.jobhunter.filter.dto;

import java.util.List;

public record MatchTestResponse(
        boolean matched,
        int score,
        int threshold,
        List<MatchedRuleDto> matchedRules
) {}
