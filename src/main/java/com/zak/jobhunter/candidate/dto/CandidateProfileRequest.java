package com.zak.jobhunter.candidate.dto;

import java.util.List;

public record CandidateProfileRequest(
        String name,
        String surname,
        String email,
        String phoneNumber,
        String location,
        String linkedinUrl,
        String githubUrl,
        String portfolioUrl,
        String currentTitle,
        Integer yearsOfExperience,
        String summary,
        String preferredJobTitles,
        String preferredLocations,
        String preferredWorkModes,
        String avoidRules,
        List<CandidateSkillDto> skills,
        List<CandidateExperienceDto> experiences
) {}
