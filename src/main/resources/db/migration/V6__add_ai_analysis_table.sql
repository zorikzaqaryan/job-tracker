-- V6: AI analysis results table

CREATE TABLE job_ai_analyses
(
    id                      BIGSERIAL PRIMARY KEY,
    job_id                  BIGINT REFERENCES jobs (id) ON DELETE CASCADE,
    provider                VARCHAR(100) NOT NULL,
    model                   VARCHAR(255) NOT NULL,
    relevant                BOOLEAN      NOT NULL,
    qualification_score     INT          NOT NULL,
    remote_compatible       BOOLEAN,
    location_compatible     BOOLEAN,
    seniority_compatible    BOOLEAN,
    matched_skills          TEXT,
    missing_important_skills TEXT,
    risk_flags              TEXT,
    decision                VARCHAR(50)  NOT NULL,
    reason                  TEXT,
    raw_request             JSONB,
    raw_response            JSONB,
    error_message           TEXT,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_job_ai_analyses_job_id ON job_ai_analyses (job_id);
CREATE INDEX idx_job_ai_analyses_decision ON job_ai_analyses (decision);
