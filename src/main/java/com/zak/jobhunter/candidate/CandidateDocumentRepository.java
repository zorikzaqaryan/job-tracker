package com.zak.jobhunter.candidate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidateDocumentRepository extends JpaRepository<CandidateDocument, Long> {
    List<CandidateDocument> findByCandidateIdAndActiveTrue(Long candidateId);
}
