package com.zak.jobhunter.enrichment;

import com.zak.jobhunter.enrichment.dto.UrlEnrichmentRequest;
import com.zak.jobhunter.enrichment.dto.UrlEnrichmentResponse;
import com.zak.jobhunter.job.JobPost;
import com.zak.jobhunter.job.JobPostRepository;
import com.zak.jobhunter.job.JobStatus;
import com.zak.jobhunter.messaging.RabbitQueues;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UrlEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(UrlEnrichmentService.class);

    private final JobPostRepository        jobPostRepository;
    private final JobUrlEnrichmentRepository enrichmentRepository;
    private final UrlContentFetcher        fetcher;
    private final ReadableTextExtractor    extractor;
    private final UrlEnrichmentProperties  properties;
    private final RabbitTemplate           rabbitTemplate;

    /**
     * Enqueue a URL enrichment request for async processing.
     */
    @Transactional
    public UrlEnrichmentResponse enqueue(Long jobId) {
        JobPost job = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found: " + jobId));

        if (job.getUrl() == null || job.getUrl().isBlank()) {
            throw new IllegalArgumentException("Job has no URL to enrich: " + jobId);
        }

        job.setStatus(JobStatus.URL_ENRICHMENT_PENDING);
        jobPostRepository.save(job);

        UrlEnrichmentRequest request = new UrlEnrichmentRequest(jobId, job.getUrl());
        rabbitTemplate.convertAndSend(
                RabbitQueues.EXCHANGE,
                RabbitQueues.URL_ENRICHMENT_ROUTING_KEY,
                request);

        log.info("[ENRICHMENT] Enqueued URL enrichment  jobId={} url={}", jobId, job.getUrl());

        JobUrlEnrichment pending = JobUrlEnrichment.builder()
                .job(job).url(job.getUrl())
                .fetchStatus(UrlEnrichmentStatus.PENDING.name())
                .build();
        return toResponse(enrichmentRepository.save(pending));
    }

    /**
     * Synchronously fetch and store URL content (used by the listener).
     */
    @Transactional
    public JobUrlEnrichment fetchAndStore(Long jobId, String url) {
        JobPost job = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found: " + jobId));

        UrlContentFetcher.FetchResult fetchResult = fetcher.fetch(url);

        JobUrlEnrichment.JobUrlEnrichmentBuilder builder = JobUrlEnrichment.builder()
                .job(job)
                .url(url)
                .fetchStatus(fetchResult.status().name())
                .httpStatus(fetchResult.httpStatus())
                .errorMessage(fetchResult.error());

        if (fetchResult.document() != null) {
            String text = extractor.extract(fetchResult.document(), properties.maxPageChars());
            String applyUrl = extractor.extractApplyUrl(fetchResult.document());
            String title = fetchResult.document().title();
            builder.extractedTitle(title)
                   .extractedText(text)
                   .extractedApplyUrl(applyUrl);
        }

        JobUrlEnrichment saved = enrichmentRepository.save(builder.build());

        JobStatus newStatus = fetchResult.status() == UrlEnrichmentStatus.SUCCESS
                ? JobStatus.URL_ENRICHED : JobStatus.URL_ENRICHMENT_FAILED;
        job.setStatus(newStatus);
        jobPostRepository.save(job);

        log.info("[ENRICHMENT] Stored result  jobId={} status={} chars={}",
                jobId, fetchResult.status(),
                saved.getExtractedText() != null ? saved.getExtractedText().length() : 0);

        return saved;
    }

    public List<UrlEnrichmentResponse> findByJobId(Long jobId) {
        return enrichmentRepository.findByJobIdOrderByCreatedAtDesc(jobId)
                .stream().map(this::toResponse).toList();
    }

    private UrlEnrichmentResponse toResponse(JobUrlEnrichment e) {
        return new UrlEnrichmentResponse(
                e.getId(),
                e.getJob() != null ? e.getJob().getId() : null,
                e.getUrl(), e.getFetchStatus(), e.getHttpStatus(),
                e.getExtractedTitle(), e.getExtractedDescription(),
                e.getExtractedApplyUrl(), e.getErrorMessage(), e.getCreatedAt());
    }
}
