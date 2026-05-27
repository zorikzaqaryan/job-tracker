package com.zak.jobhunter.messaging;

/**
 * Central registry of RabbitMQ exchange, queue, and routing-key names.
 * Use these constants everywhere to avoid magic strings.
 */
public final class RabbitQueues {

    private RabbitQueues() {}

    public static final String EXCHANGE = "jobs.exchange";
    public static final String DLX      = "jobs.dlx";

    // ── Raw job messages ──────────────────────────────────────────────────
    public static final String RAW_QUEUE       = "raw-job-messages";
    public static final String RAW_DLQ         = "raw-job-messages.dlq";
    public static final String RAW_ROUTING_KEY = "jobs.raw";

    // ── Matched jobs ──────────────────────────────────────────────────────
    public static final String MATCHED_QUEUE       = "matched-jobs";
    public static final String MATCHED_DLQ         = "matched-jobs.dlq";
    public static final String MATCHED_ROUTING_KEY = "jobs.matched";

    // ── AI analysis ───────────────────────────────────────────────────────
    public static final String AI_ANALYSIS_QUEUE       = "ai-job-analysis-requests";
    public static final String AI_ANALYSIS_DLQ         = "ai-job-analysis-requests.dlq";
    public static final String AI_ANALYSIS_ROUTING_KEY = "jobs.ai.analysis";

    public static final String AI_RESULT_QUEUE       = "ai-job-analysis-results";
    public static final String AI_RESULT_DLQ         = "ai-job-analysis-results.dlq";
    public static final String AI_RESULT_ROUTING_KEY = "jobs.ai.result";

    // ── URL enrichment ────────────────────────────────────────────────────
    public static final String URL_ENRICHMENT_QUEUE       = "job-url-enrichment-requests";
    public static final String URL_ENRICHMENT_DLQ         = "job-url-enrichment-requests.dlq";
    public static final String URL_ENRICHMENT_ROUTING_KEY = "jobs.url.enrichment";
}
