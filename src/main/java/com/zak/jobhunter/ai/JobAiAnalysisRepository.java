package com.zak.jobhunter.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobAiAnalysisRepository extends JpaRepository<JobAiAnalysis, Long> {
    List<JobAiAnalysis> findByJobIdOrderByCreatedAtDesc(Long jobId);
    Optional<JobAiAnalysis> findFirstByJobIdOrderByCreatedAtDesc(Long jobId);
}
