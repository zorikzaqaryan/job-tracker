package com.zak.jobhunter.ai.dto;

import java.util.List;

/**
 * Summarised candidate context passed to the AI prompt.
 * Does NOT include personal data (email, phone) to minimise exposure.
 */
public record CandidateContextDto(
        String currentTitle,
        String location,
        Integer yearsOfExperience,
        String summary,
        String preferredJobTitles,
        String preferredLocations,
        String preferredWorkModes,
        String avoidRules,
        List<String> skills
) {}
