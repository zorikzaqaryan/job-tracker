-- V1: Initial schema

-- ─────────────────────────────────────────
-- 1. job_sources
-- ─────────────────────────────────────────
CREATE TABLE job_sources
(
    id                       BIGSERIAL PRIMARY KEY,
    source_type              VARCHAR(50)  NOT NULL,
    name                     VARCHAR(255) NOT NULL,
    telegram_username        VARCHAR(255),
    telegram_channel_id      VARCHAR(100),
    url                      TEXT,
    enabled                  BOOLEAN      NOT NULL DEFAULT TRUE,
    last_external_message_id VARCHAR(255),
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_job_sources_enabled_type ON job_sources (enabled, source_type);

-- ─────────────────────────────────────────
-- 2. filter_rules
-- ─────────────────────────────────────────
CREATE TABLE filter_rules
(
    id                 BIGSERIAL PRIMARY KEY,
    keyword            VARCHAR(255) NOT NULL,
    normalized_keyword VARCHAR(255) NOT NULL,
    match_type         VARCHAR(50)  NOT NULL,
    field              VARCHAR(50)  NOT NULL,
    rule_type          VARCHAR(50)  NOT NULL,
    weight             INT          NOT NULL,
    enabled            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_filter_rules_enabled ON filter_rules (enabled);

-- ─────────────────────────────────────────
-- 3. raw_messages
-- ─────────────────────────────────────────
CREATE TABLE raw_messages
(
    id                  BIGSERIAL PRIMARY KEY,
    source_id           BIGINT REFERENCES job_sources (id) ON DELETE SET NULL,
    source_type         VARCHAR(50)  NOT NULL,
    source_name         VARCHAR(255),
    source_channel_id   VARCHAR(100),
    external_message_id VARCHAR(255) NOT NULL,
    raw_text            TEXT         NOT NULL,
    url                 TEXT,
    published_at        TIMESTAMPTZ,
    content_hash        VARCHAR(64)  NOT NULL,
    processed           BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_raw_messages_identity UNIQUE (source_type, source_channel_id, external_message_id)
);

CREATE INDEX idx_raw_messages_processed ON raw_messages (processed);
CREATE INDEX idx_raw_messages_content_hash ON raw_messages (content_hash);

-- ─────────────────────────────────────────
-- 4. jobs
-- ─────────────────────────────────────────
CREATE TABLE jobs
(
    id             BIGSERIAL PRIMARY KEY,
    raw_message_id BIGINT REFERENCES raw_messages (id) ON DELETE SET NULL,
    title          TEXT,
    company        VARCHAR(255),
    location       VARCHAR(255),
    description    TEXT,
    url            TEXT,
    score          INT         NOT NULL DEFAULT 0,
    status         VARCHAR(50) NOT NULL,
    content_hash   VARCHAR(64) NOT NULL UNIQUE,
    sent_at        TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_jobs_status ON jobs (status);
CREATE INDEX idx_jobs_created_at ON jobs (created_at DESC);

-- ─────────────────────────────────────────
-- 5. job_rule_matches
-- ─────────────────────────────────────────
CREATE TABLE job_rule_matches
(
    id            BIGSERIAL PRIMARY KEY,
    job_id        BIGINT REFERENCES jobs (id) ON DELETE CASCADE,
    rule_id       BIGINT REFERENCES filter_rules (id) ON DELETE SET NULL,
    matched_field VARCHAR(50) NOT NULL,
    matched_text  TEXT,
    weight        INT         NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_job_rule_matches_job_id ON job_rule_matches (job_id);
