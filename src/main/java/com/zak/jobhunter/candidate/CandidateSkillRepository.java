package com.zak.jobhunter.candidate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidateSkillRepository extends JpaRepository<CandidateSkill, Long> {
    List<CandidateSkill> findByCandidateIdOrderBySkillName(Long candidateId);
    void deleteByCandidateId(Long candidateId);
}
