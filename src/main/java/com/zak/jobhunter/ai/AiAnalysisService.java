package com.zak.jobhunter.ai;

import com.zak.jobhunter.ai.dto.AiJobAnalysisRequest;
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

/**
 * Orchestrates the AI analysis pipeline from the producer side.
 *
 * <p>Called by {@link com.zak.jobhunter.job.JobService} after rule-based matching succeeds.
 * Publishes an {@link AiJobAnalysisRequest} to RabbitMQ and updates job status to
 * {@link JobStatus#AI_ANALYSIS_PENDING}.
 */
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);

    private final AiProperties      aiProperties;
    private final JobPostRepository jobPostRepository;
    private final RabbitTemplate    rabbitTemplate;

    /**
     * Marks a job as pending AI analysis and enqueues the analysis request.
     *
     * @param job             persisted job that passed rule-based matching
     * @param matchedKeywords keywords matched by the rule engine
     */
    @Transactional
    public void enqueueForAnalysis(JobPost job, List<String> matchedKeywords) {
        if (!aiProperties.enabled()) {
            log.debug("[AI-SERVICE] AI disabled — skipping for jobId={}", job.getId());
            return;
        }

        job.setStatus(JobStatus.AI_ANALYSIS_PENDING);
        jobPostRepository.save(job);

        AiJobAnalysisRequest request = new AiJobAnalysisRequest(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.getDescription(),
                job.getUrl(),
                job.getScore(),
                matchedKeywords
        );

        rabbitTemplate.convertAndSend(
                RabbitQueues.EXCHANGE,
                RabbitQueues.AI_ANALYSIS_ROUTING_KEY,
                request);

        log.info("[AI-SERVICE] ▶ Enqueued AI analysis  jobId={} score={}", job.getId(), job.getScore());
    }

    /**
     * Manually trigger AI analysis for an existing job (e.g. via REST API retry).
     */
    @Transactional
    public void retrigger(Long jobId) {
        JobPost job = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found: " + jobId));
        enqueueForAnalysis(job, List.of());
    }
}
