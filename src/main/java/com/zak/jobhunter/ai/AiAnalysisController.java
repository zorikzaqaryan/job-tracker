package com.zak.jobhunter.ai;

import com.zak.jobhunter.ai.dto.JobAiAnalysisResult;
import com.zak.jobhunter.ai.dto.CandidateContextDto;
import com.zak.jobhunter.ai.dto.JobContextDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Tag(name = "AI Analysis", description = "Trigger and inspect AI job qualification analysis")
public class AiAnalysisController {

    private final AiAnalysisService        aiAnalysisService;
    private final JobAiAnalysisRepository  analysisRepository;
    private final JobAiAnalyzer            jobAiAnalyzer;

    @PostMapping("/api/jobs/{jobId}/ai/analyze")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Enqueue AI analysis for an existing job")
    public Map<String, Object> enqueue(@PathVariable Long jobId) {
        aiAnalysisService.retrigger(jobId);
        return Map.of("jobId", jobId, "message", "AI analysis request enqueued");
    }

    @PostMapping("/api/jobs/{jobId}/ai/retry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Retry AI analysis for a job (e.g. after ANALYSIS_FAILED)")
    public Map<String, Object> retry(@PathVariable Long jobId) {
        aiAnalysisService.retrigger(jobId);
        return Map.of("jobId", jobId, "message", "AI analysis re-enqueued");
    }

    @GetMapping("/api/jobs/{jobId}/ai-analyses")
    @Operation(summary = "List all AI analyses for a job")
    public List<AiAnalysisResponse> listByJob(@PathVariable Long jobId) {
        return analysisRepository.findByJobIdOrderByCreatedAtDesc(jobId)
                .stream().map(this::toResponse).toList();
    }

    @GetMapping("/api/ai-analyses/{id}")
    @Operation(summary = "Get a specific AI analysis record")
    public AiAnalysisResponse getById(@PathVariable Long id) {
        return toResponse(analysisRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Analysis not found: " + id)));
    }

    @PostMapping("/api/ai/test-job-match")
    @Operation(summary = "Ad-hoc test: analyze a job description against the active candidate profile")
    public JobAiAnalysisResult testMatch(@RequestBody TestMatchRequest req) {
        JobContextDto jobCtx = new JobContextDto(
                null, req.title(), req.company(), req.location(),
                req.description(), null, null,
                0, List.of());
        CandidateContextDto candidateCtx = new CandidateContextDto(
                "Senior Java Backend Developer", "Yerevan, Armenia", 9,
                null, req.preferredJobTitles() != null ? req.preferredJobTitles() : "Senior Java Backend",
                "Remote, Worldwide", "Remote, Full Remote",
                "US-only, onsite-only",
                List.of("Java", "Spring Boot", "Kafka", "PostgreSQL", "Docker"));
        return jobAiAnalyzer.analyze(jobCtx, candidateCtx);
    }

    // ── inner DTOs ────────────────────────────────────────────────────────

    public record TestMatchRequest(
            String title,
            String company,
            String location,
            String description,
            String preferredJobTitles,
            Long candidateProfileId
    ) {}

    public record AiAnalysisResponse(
            Long id,
            Long jobId,
            String provider,
            String model,
            boolean relevant,
            int qualificationScore,
            Boolean remoteCompatible,
            Boolean locationCompatible,
            Boolean seniorityCompatible,
            String matchedSkills,
            String missingImportantSkills,
            String riskFlags,
            String decision,
            String reason,
            String errorMessage,
            java.time.Instant createdAt
    ) {}

    private AiAnalysisResponse toResponse(JobAiAnalysis a) {
        return new AiAnalysisResponse(
                a.getId(),
                a.getJob() != null ? a.getJob().getId() : null,
                a.getProvider(), a.getModel(),
                a.isRelevant(), a.getQualificationScore(),
                a.getRemoteCompatible(), a.getLocationCompatible(), a.getSeniorityCompatible(),
                a.getMatchedSkills(), a.getMissingImportantSkills(), a.getRiskFlags(),
                a.getDecision(), a.getReason(), a.getErrorMessage(), a.getCreatedAt());
    }
}
