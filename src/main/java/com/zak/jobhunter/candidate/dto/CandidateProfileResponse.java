package com.zak.jobhunter.candidate.dto;

import java.time.Instant;
import java.util.List;

public record CandidateProfileResponse(
        Long id,
        String name,
        String surname,
        String currentTitle,
        String location,
        Integer yearsOfExperience,
        String summary,
        String preferredJobTitles,
        String preferredLocations,
        String preferredWorkModes,
        String avoidRules,
        String linkedinUrl,
        String githubUrl,
        String portfolioUrl,
        List<CandidateSkillDto> skills,
        List<CandidateExperienceDto> experiences,
        Instant createdAt,
        Instant updatedAt
) {}
