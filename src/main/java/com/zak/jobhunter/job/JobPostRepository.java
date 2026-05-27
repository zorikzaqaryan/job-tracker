package com.zak.jobhunter.job;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobPostRepository extends JpaRepository<JobPost, Long> {

    Optional<JobPost> findByContentHash(String contentHash);

    boolean existsByContentHash(String contentHash);

    Page<JobPost> findByStatus(JobStatus status, Pageable pageable);

    Page<JobPost> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
