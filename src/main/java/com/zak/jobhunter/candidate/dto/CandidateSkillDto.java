package com.zak.jobhunter.candidate.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CandidateSkillDto(
        Long id,
        @NotBlank String skillName,
        String level,
        BigDecimal yearsOfExperience
) {}
