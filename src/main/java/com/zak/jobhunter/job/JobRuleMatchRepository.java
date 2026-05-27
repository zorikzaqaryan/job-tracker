package com.zak.jobhunter.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRuleMatchRepository extends JpaRepository<JobRuleMatch, Long> {

    List<JobRuleMatch> findByJobId(Long jobId);
}
