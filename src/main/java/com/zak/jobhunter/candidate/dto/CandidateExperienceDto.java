package com.zak.jobhunter.candidate.dto;

import java.time.LocalDate;

public record CandidateExperienceDto(
        Long id,
        String company,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        String technologies
) {}
