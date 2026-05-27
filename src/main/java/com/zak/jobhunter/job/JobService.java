package com.zak.jobhunter.job;

import com.zak.jobhunter.ai.AiAnalysisService;
import com.zak.jobhunter.ai.AiProperties;
import com.zak.jobhunter.common.HashUtils;
import com.zak.jobhunter.filter.FilterRuleRepository;
import com.zak.jobhunter.filter.MatchingService;
import com.zak.jobhunter.filter.dto.MatchedRuleDto;
import com.zak.jobhunter.ingestion.RawMessage;
import com.zak.jobhunter.job.dto.JobResponse;
import com.zak.jobhunter.job.dto.JobSearchResponse;
import com.zak.jobhunter.metrics.JobHunterMetrics;
import com.zak.jobhunter.telegram.TelegramBotSender;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final JobPostRepository      jobPostRepository;
    private final JobRuleMatchRepository jobRuleMatchRepository;
    private final FilterRuleRepository   filterRuleRepository;
    private final MatchingService        matchingService;
    private final TelegramBotSender      telegramBotSender;
    private final JobHunterMetrics       metrics;
    private final AiAnalysisService      aiAnalysisService;
    private final AiProperties           aiProperties;

    // ── Query ─────────────────────────────────────────────────────────────

    public JobSearchResponse findAll(Pageable pageable) {
        return toSearchResponse(jobPostRepository.findAllByOrderByCreatedAtDesc(pageable));
    }

    public JobResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    public JobSearchResponse findByStatus(JobStatus status, Pageable pageable) {
        return toSearchResponse(jobPostRepository.findByStatus(status, pageable));
    }

    // ── Processing ────────────────────────────────────────────────────────

    /**
     * Normalise a raw message into a {@link JobPost}, apply rule-based matching,
     * persist the result, and route to the AI pipeline or Telegram directly.
     *
     * <p>Processing pipeline:
     * <ol>
     *   <li>Deduplicate by content hash.</li>
     *   <li>Extract lightweight fields (title, location).</li>
     *   <li>Run rule-based {@link MatchingService}.</li>
     *   <li>If score &lt; threshold → status = NOT_MATCHED; done.</li>
     *   <li>If score ≥ threshold AND AI enabled → status = MATCHED_BY_RULES → enqueue AI.</li>
     *   <li>If score ≥ threshold AND AI disabled → status = MATCHED → send to Telegram.</li>
     * </ol>
     */
    @Transactional
    public JobPost processRawMessage(RawMessage rawMessage) {
        String contentHash = HashUtils.jobContentHash(
                null, null, null, rawMessage.getUrl(), rawMessage.getRawText());

        if (jobPostRepository.existsByContentHash(contentHash)) {
            log.info("[JOB] ♻ Duplicate job content — skipping  rawId={} hash={}",
                    rawMessage.getId(), contentHash.substring(0, 12) + "...");
            return jobPostRepository.findByContentHash(contentHash).orElseThrow();
        }

        String rawText    = rawMessage.getRawText();
        String title      = extractTitle(rawText);
        String location   = extractLocation(rawText);
        String description = rawText;

        String channel = rawMessage.getSourceName() != null
                ? rawMessage.getSourceName() : rawMessage.getSourceChannelId();

        log.info("[JOB] Evaluating  channel='{}' title='{}'", channel, title);

        // ── Rule-based matching ───────────────────────────────────────────
        MatchingService.MatchResult result = matchingService.evaluate(title, description, location);
        int threshold = matchingService.getThreshold();
        boolean matched = result.isMatched(threshold);

        List<String> hitLabels = result.matchedRules().stream()
                .map(r -> r.keyword() + "(" + r.weight() + ")")
                .toList();
        log.info("[JOB] Score={}  threshold={}  matched={}  rules={}  channel='{}'",
                result.score(), threshold, matched, hitLabels, channel);

        JobStatus status = matched ? JobStatus.MATCHED_BY_RULES : JobStatus.NOT_MATCHED;

        JobPost job = JobPost.builder()
                .rawMessage(rawMessage)
                .title(title)
                .location(location)
                .description(description)
                .url(rawMessage.getUrl())
                .score(result.score())
                .status(status)
                .contentHash(contentHash)
                .build();

        JobPost savedJob = jobPostRepository.save(job);

        List<JobRuleMatch> ruleMatches = buildRuleMatches(savedJob, result.matchedRules());
        ruleMatches.forEach(m -> m.setJob(savedJob));
        jobRuleMatchRepository.saveAll(ruleMatches);
        savedJob.getRuleMatches().addAll(ruleMatches);

        String sourceType = rawMessage.getSourceType() != null ? rawMessage.getSourceType() : "unknown";
        metrics.recordEvaluated(sourceType, channel, matched ? "matched" : "not_matched", result.score());

        if (!matched) {
            log.info("[JOB] ✘ NOT matched  jobId={} score={}  channel='{}'",
                    savedJob.getId(), savedJob.getScore(), channel);
            return savedJob;
        }

        // ── Route matched job ─────────────────────────────────────────────
        List<String> matchedKeywords = result.matchedRules().stream()
                .map(MatchedRuleDto::keyword).toList();

        if (aiProperties.enabled() &&
                savedJob.getScore() >= aiProperties.minRuleScoreBeforeAi()) {
            // Route through AI pipeline
            log.info("[JOB] ✔ MATCHED_BY_RULES — routing to AI  jobId={} score={} channel='{}'",
                    savedJob.getId(), savedJob.getScore(), channel);
            aiAnalysisService.enqueueForAnalysis(savedJob, matchedKeywords);
        } else {
            // Legacy direct send (AI disabled or score below AI threshold)
            log.info("[JOB] ✔ MATCHED (AI skipped) — sending to Telegram  jobId={} score={} channel='{}'",
                    savedJob.getId(), savedJob.getScore(), channel);
            savedJob.setStatus(JobStatus.MATCHED);
            jobPostRepository.save(savedJob);
            sendToTelegram(savedJob, ruleMatches, channel);
        }

        return savedJob;
    }

    @Transactional
    public JobResponse setStatus(Long id, JobStatus status) {
        JobPost job = getOrThrow(id);
        job.setStatus(status);
        return toResponse(jobPostRepository.save(job));
    }

    @Transactional
    public JobResponse resend(Long id) {
        JobPost job = getOrThrow(id);
        List<JobRuleMatch> matches = jobRuleMatchRepository.findByJobId(id);
        String channel = job.getRawMessage() != null && job.getRawMessage().getSourceName() != null
                ? job.getRawMessage().getSourceName() : "unknown";
        sendToTelegram(job, matches, channel);
        return toResponse(jobPostRepository.save(job));
    }

    // ── private helpers ───────────────────────────────────────────────────

    private void sendToTelegram(JobPost job, List<JobRuleMatch> matches, String channel) {
        try {
            telegramBotSender.sendMatchedJob(job, matches);
            job.setStatus(JobStatus.SENT);
            job.setSentAt(Instant.now());
            jobPostRepository.save(job);
            metrics.recordSent(channel);
            log.info("[TELEGRAM] ✔ Sent  jobId={} title='{}'", job.getId(), job.getTitle());
        } catch (TelegramBotSender.TelegramSendException ex) {
            log.error("[TELEGRAM] ✘ Send failed  jobId={} title='{}' error={}",
                    job.getId(), job.getTitle(), ex.getMessage());
            job.setStatus(JobStatus.SEND_FAILED);
            jobPostRepository.save(job);
            metrics.recordSendFailed(channel);
        }
    }

    private List<JobRuleMatch> buildRuleMatches(JobPost job, List<MatchedRuleDto> dtos) {
        return dtos.stream().map(dto -> {
            var rule = dto.ruleId() != null
                    ? filterRuleRepository.findById(dto.ruleId()).orElse(null)
                    : null;
            return JobRuleMatch.builder()
                    .job(job)
                    .rule(rule)
                    .matchedField(dto.matchedField())
                    .matchedText(dto.matchedText())
                    .weight(dto.weight())
                    .build();
        }).toList();
    }

    private String extractTitle(String text) {
        if (text == null) return null;
        return text.lines()
                .map(String::trim)
                .filter(l -> !l.isBlank() && l.length() > 5)
                .findFirst()
                .orElse(null);
    }

    private String extractLocation(String text) {
        if (text == null) return null;
        String lower = text.toLowerCase();
        StringBuilder locations = new StringBuilder();
        if (lower.contains("remote"))    locations.append("Remote ");
        if (lower.contains("worldwide") || lower.contains("world wide")) {
            locations.append("Worldwide ");
        }
        if (lower.contains("yerevan"))   locations.append("Yerevan ");
        if (lower.contains("armenia"))   locations.append("Armenia ");
        String result = locations.toString().trim();
        return result.isBlank() ? null : result;
    }

    private JobPost getOrThrow(Long id) {
        return jobPostRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job not found: " + id));
    }

    private JobSearchResponse toSearchResponse(Page<JobPost> page) {
        return new JobSearchResponse(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private JobResponse toResponse(JobPost job) {
        List<JobRuleMatch> matches = jobRuleMatchRepository.findByJobId(job.getId());
        List<JobResponse.RuleMatchResponse> matchDtos = matches.stream()
                .map(m -> new JobResponse.RuleMatchResponse(
                        m.getRule() != null ? m.getRule().getId() : null,
                        m.getMatchedField(), m.getMatchedText(), m.getWeight()))
                .toList();
        return new JobResponse(
                job.getId(),
                job.getRawMessage() != null ? job.getRawMessage().getId() : null,
                job.getTitle(), job.getCompany(), job.getLocation(), job.getDescription(),
                job.getUrl(), job.getScore(), job.getStatus(), job.getContentHash(),
                job.getSentAt(), job.getCreatedAt(), job.getUpdatedAt(), matchDtos);
    }
}
