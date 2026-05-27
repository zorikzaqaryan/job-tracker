package com.zak.jobhunter.job;

public enum JobStatus {

    // ── Lifecycle ─────────────────────────────────────────────────────────
    NEW,
    NOT_MATCHED,
    IGNORED,

    // ── Rule-based matching ───────────────────────────────────────────────
    /** Score ≥ threshold; awaiting second-stage AI analysis. */
    MATCHED_BY_RULES,

    /**
     * Legacy: matched by rules and sent directly to Telegram (before AI pipeline).
     * Kept for backward compatibility with existing records.
     */
    MATCHED,

    // ── URL enrichment ────────────────────────────────────────────────────
    URL_ENRICHMENT_PENDING,
    URL_ENRICHED,
    URL_ENRICHMENT_FAILED,

    // ── AI analysis ───────────────────────────────────────────────────────
    AI_ANALYSIS_PENDING,
    AI_MATCHED,
    AI_REJECTED,
    AI_ANALYSIS_FAILED,

    // ── Output ────────────────────────────────────────────────────────────
    SENT,
    SEND_FAILED,

    // ── Application tracking ──────────────────────────────────────────────
    APPLICATION_CANDIDATE
}
