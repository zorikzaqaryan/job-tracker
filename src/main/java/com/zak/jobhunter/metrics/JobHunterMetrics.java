package com.zak.jobhunter.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Central facade for all custom Micrometer metrics.
 *
 * <p>Metric naming follows the Prometheus/Micrometer convention:
 * {@code jobhunter.<noun>.<verb>} — dots become underscores in Prometheus output.
 *
 * <h3>Exposed metrics</h3>
 * <pre>
 *  jobhunter_messages_ingested_total   {source_type, channel}
 *  jobhunter_messages_duplicate_total  {source_type, channel, reason}
 *  jobhunter_jobs_evaluated_total      {source_type, channel, result}
 *  jobhunter_matching_score            {source_type, channel}  ← histogram
 *  jobhunter_jobs_sent_total           {channel}
 *  jobhunter_jobs_send_failed_total    {channel}
 * </pre>
 *
 * <p>A common {@code application=job-hunter} tag is added globally via
 * {@code management.metrics.tags.application} in {@code application.yml}.
 */
@Component
public class JobHunterMetrics {

    private final MeterRegistry registry;

    public JobHunterMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    // ── Ingestion ─────────────────────────────────────────────────────────

    /**
     * A new unique message was accepted and queued for processing.
     *
     * @param sourceType e.g. "telegram"
     * @param channel    human-readable channel name (e.g. "remoteyeah")
     */
    public void recordIngested(String sourceType, String channel) {
        Counter.builder("jobhunter.messages.ingested")
                .description("Total messages accepted from a source channel")
                .tag("source_type", safe(sourceType))
                .tag("channel", safe(channel))
                .register(registry)
                .increment();
    }

    /**
     * A message was skipped because it duplicates something already stored.
     *
     * @param reason "msgid" (exact ID match) or "content_hash" (same body, different ID)
     */
    public void recordDuplicate(String sourceType, String channel, String reason) {
        Counter.builder("jobhunter.messages.duplicate")
                .description("Total duplicate messages skipped")
                .tag("source_type", safe(sourceType))
                .tag("channel", safe(channel))
                .tag("reason", safe(reason))
                .register(registry)
                .increment();
    }

    // ── Matching / Scoring ────────────────────────────────────────────────

    /**
     * A job was scored by the matching engine.
     * Increments both the evaluated counter and the score histogram.
     *
     * @param result "matched" or "not_matched"
     * @param score  raw numeric score from the engine
     */
    public void recordEvaluated(String sourceType, String channel, String result, int score) {
        Counter.builder("jobhunter.jobs.evaluated")
                .description("Total jobs evaluated by the matching engine")
                .tag("source_type", safe(sourceType))
                .tag("channel", safe(channel))
                .tag("result", result)
                .register(registry)
                .increment();

        DistributionSummary.builder("jobhunter.matching.score")
                .description("Distribution of matching scores produced by the engine")
                .tag("source_type", safe(sourceType))
                .tag("channel", safe(channel))
                .register(registry)
                .record(score);
    }

    // ── Telegram output ───────────────────────────────────────────────────

    /** Job was successfully forwarded to the Telegram output channel. */
    public void recordSent(String channel) {
        Counter.builder("jobhunter.jobs.sent")
                .description("Total jobs successfully forwarded to Telegram")
                .tag("channel", safe(channel))
                .register(registry)
                .increment();
    }

    /** Telegram send call failed (will be retried or marked SEND_FAILED). */
    public void recordSendFailed(String channel) {
        Counter.builder("jobhunter.jobs.send_failed")
                .description("Total jobs that failed to send to Telegram")
                .tag("channel", safe(channel))
                .register(registry)
                .increment();
    }

    // ─────────────────────────────────────────────────────────────────────

    private static String safe(String value) {
        return (value != null && !value.isBlank()) ? value : "unknown";
    }
}
