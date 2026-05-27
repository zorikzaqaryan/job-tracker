-- V7: URL enrichment results table

CREATE TABLE job_url_enrichments
(
    id                    BIGSERIAL PRIMARY KEY,
    job_id                BIGINT REFERENCES jobs (id) ON DELETE CASCADE,
    url                   TEXT        NOT NULL,
    fetch_status          VARCHAR(50) NOT NULL,
    http_status           INT,
    extracted_title       TEXT,
    extracted_description TEXT,
    extracted_apply_url   TEXT,
    extracted_text        TEXT,
    error_message         TEXT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_job_url_enrichments_job_id ON job_url_enrichments (job_id);
CREATE INDEX idx_job_url_enrichments_fetch_status ON job_url_enrichments (fetch_status);
