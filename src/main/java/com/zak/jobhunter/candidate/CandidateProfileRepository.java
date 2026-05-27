package com.zak.jobhunter.candidate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long> {

    /** Returns the first (most recently created) active candidate profile for AI prompts. */
    @Query("SELECT c FROM CandidateProfile c ORDER BY c.createdAt DESC LIMIT 1")
    Optional<CandidateProfile> findMostRecent();
}
