package com.zak.jobhunter.enrichment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobUrlEnrichmentRepository extends JpaRepository<JobUrlEnrichment, Long> {
    List<JobUrlEnrichment> findByJobIdOrderByCreatedAtDesc(Long jobId);
    Optional<JobUrlEnrichment> findFirstByJobIdAndFetchStatus(Long jobId, String fetchStatus);
}
