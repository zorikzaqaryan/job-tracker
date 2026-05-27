package com.zak.jobhunter.enrichment;

import com.zak.jobhunter.enrichment.dto.UrlEnrichmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "URL Enrichment", description = "Fetch and store additional context from job posting URLs")
public class UrlEnrichmentController {

    private final UrlEnrichmentService enrichmentService;

    @PostMapping("/api/jobs/{jobId}/enrich-url")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Enqueue URL enrichment for a job")
    public UrlEnrichmentResponse enqueue(@PathVariable Long jobId) {
        return enrichmentService.enqueue(jobId);
    }

    @GetMapping("/api/jobs/{jobId}/url-enrichments")
    @Operation(summary = "List all URL enrichment records for a job")
    public List<UrlEnrichmentResponse> list(@PathVariable Long jobId) {
        return enrichmentService.findByJobId(jobId);
    }
}
