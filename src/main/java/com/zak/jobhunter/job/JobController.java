package com.zak.jobhunter.job;

import com.zak.jobhunter.job.dto.JobResponse;
import com.zak.jobhunter.job.dto.JobSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Job post management and manual actions")
public class JobController {

    private final JobService service;

    @GetMapping
    @Operation(summary = "List all jobs (paginated)")
    public JobSearchResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.findAll(PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job by ID")
    public JobResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/matched")
    @Operation(summary = "List MATCHED jobs")
    public JobSearchResponse matched(Pageable pageable) {
        return service.findByStatus(JobStatus.MATCHED, pageable);
    }

    @GetMapping("/sent")
    @Operation(summary = "List SENT jobs")
    public JobSearchResponse sent(Pageable pageable) {
        return service.findByStatus(JobStatus.SENT, pageable);
    }

    @GetMapping("/ignored")
    @Operation(summary = "List IGNORED jobs")
    public JobSearchResponse ignored(Pageable pageable) {
        return service.findByStatus(JobStatus.IGNORED, pageable);
    }

    @PatchMapping("/{id}/ignore")
    @Operation(summary = "Mark a job as IGNORED")
    public JobResponse ignore(@PathVariable Long id) {
        return service.setStatus(id, JobStatus.IGNORED);
    }

    @PatchMapping("/{id}/mark-good")
    @Operation(summary = "Manually mark a job as MATCHED")
    public JobResponse markGood(@PathVariable Long id) {
        return service.setStatus(id, JobStatus.MATCHED);
    }

    @PatchMapping("/{id}/mark-bad")
    @Operation(summary = "Manually mark a job as NOT_MATCHED")
    public JobResponse markBad(@PathVariable Long id) {
        return service.setStatus(id, JobStatus.NOT_MATCHED);
    }

    @PostMapping("/{id}/resend")
    @Operation(summary = "Resend a job to Telegram")
    public JobResponse resend(@PathVariable Long id) {
        return service.resend(id);
    }
}
