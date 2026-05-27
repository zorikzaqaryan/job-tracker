package com.zak.jobhunter.candidate.dto;

import java.time.Instant;

public record CandidateDocumentResponse(
        Long id,
        String documentType,
        String fileName,
        String contentType,
        boolean active,
        Instant createdAt
) {}
