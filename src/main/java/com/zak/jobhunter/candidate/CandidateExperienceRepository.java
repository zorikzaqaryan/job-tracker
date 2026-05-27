package com.zak.jobhunter.candidate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidateExperienceRepository extends JpaRepository<CandidateExperience, Long> {
    List<CandidateExperience> findByCandidateIdOrderByStartDateDesc(Long candidateId);
}
