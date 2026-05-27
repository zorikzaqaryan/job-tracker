package com.zak.jobhunter.job.dto;

import java.util.List;

public record JobSearchResponse(
        List<JobResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
