-- V5: Candidate profile, skills, experience, and document tables

CREATE TABLE candidate_profiles
(
    id                    BIGSERIAL PRIMARY KEY,
    name                  VARCHAR(255),
    surname               VARCHAR(255),
    email                 VARCHAR(255),
    phone_number          VARCHAR(100),
    location              VARCHAR(255),
    linkedin_url          TEXT,
    github_url            TEXT,
    portfolio_url         TEXT,
    current_title         VARCHAR(255),
    years_of_experience   INT,
    summary               TEXT,
    preferred_job_titles  TEXT,
    preferred_locations   TEXT,
    preferred_work_modes  TEXT,
    avoid_rules           TEXT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE candidate_skills
(
    id                  BIGSERIAL PRIMARY KEY,
    candidate_id        BIGINT REFERENCES candidate_profiles (id) ON DELETE CASCADE,
    skill_name          VARCHAR(255) NOT NULL,
    level               VARCHAR(50),
    years_of_experience NUMERIC(4, 1)
);

CREATE INDEX idx_candidate_skills_candidate_id ON candidate_skills (candidate_id);

CREATE TABLE candidate_experiences
(
    id           BIGSERIAL PRIMARY KEY,
    candidate_id BIGINT REFERENCES candidate_profiles (id) ON DELETE CASCADE,
    company      VARCHAR(255),
    title        VARCHAR(255),
    start_date   DATE,
    end_date     DATE,
    description  TEXT,
    technologies TEXT
);

CREATE INDEX idx_candidate_experiences_candidate_id ON candidate_experiences (candidate_id);

CREATE TABLE candidate_documents
(
    id            BIGSERIAL PRIMARY KEY,
    candidate_id  BIGINT REFERENCES candidate_profiles (id) ON DELETE CASCADE,
    document_type VARCHAR(50)  NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    content_type  VARCHAR(255),
    storage_path  TEXT,
    checksum      VARCHAR(64),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_candidate_documents_candidate_id ON candidate_documents (candidate_id);
