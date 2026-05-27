-- V2: Seed default filter rules

INSERT INTO filter_rules (keyword, normalized_keyword, match_type, field, rule_type, weight, enabled)
VALUES
    -- ── Positive rules ────────────────────────────────────────────────────────
    ('Remote',       'remote',       'WHOLE_WORD', 'ANY',         'POSITIVE', 5,  TRUE),
    ('Full Remote',  'full remote',  'PHRASE',     'ANY',         'POSITIVE', 6,  TRUE),
    ('Yerevan',      'yerevan',      'WHOLE_WORD', 'ANY',         'POSITIVE', 5,  TRUE),
    ('Armenia',      'armenia',      'WHOLE_WORD', 'ANY',         'POSITIVE', 5,  TRUE),
    ('Worldwide',    'worldwide',    'WHOLE_WORD', 'ANY',         'POSITIVE', 5,  TRUE),
    ('World Wide',   'world wide',   'PHRASE',     'ANY',         'POSITIVE', 5,  TRUE),
    ('Java',         'java',         'WHOLE_WORD', 'TITLE',       'POSITIVE', 4,  TRUE),
    ('Spring Boot',  'spring boot',  'PHRASE',     'ANY',         'POSITIVE', 4,  TRUE),
    ('Backend',      'backend',      'WHOLE_WORD', 'TITLE',       'POSITIVE', 3,  TRUE),
    ('Senior',       'senior',       'WHOLE_WORD', 'TITLE',       'POSITIVE', 3,  TRUE),
    ('Lead',         'lead',         'WHOLE_WORD', 'TITLE',       'POSITIVE', 3,  TRUE),
    ('Kafka',        'kafka',        'WHOLE_WORD', 'ANY',         'POSITIVE', 2,  TRUE),
    ('PostgreSQL',   'postgresql',   'WHOLE_WORD', 'ANY',         'POSITIVE', 2,  TRUE),
    ('AWS',          'aws',          'WHOLE_WORD', 'ANY',         'POSITIVE', 2,  TRUE),
    ('GCP',          'gcp',          'WHOLE_WORD', 'ANY',         'POSITIVE', 2,  TRUE),
    ('Kubernetes',   'kubernetes',   'WHOLE_WORD', 'ANY',         'POSITIVE', 2,  TRUE),
    -- ── Negative rules ────────────────────────────────────────────────────────
    ('US only',      'us only',      'PHRASE',     'ANY',         'NEGATIVE', -10, TRUE),
    ('Canada only',  'canada only',  'PHRASE',     'ANY',         'NEGATIVE', -10, TRUE),
    ('onsite only',  'onsite only',  'PHRASE',     'ANY',         'NEGATIVE', -10, TRUE),
    ('internship',   'internship',   'WHOLE_WORD', 'ANY',         'NEGATIVE', -10, TRUE),
    ('unpaid',       'unpaid',       'WHOLE_WORD', 'ANY',         'NEGATIVE', -10, TRUE),
    ('WordPress',    'wordpress',    'WHOLE_WORD', 'ANY',         'NEGATIVE', -5,  TRUE),
    ('PHP only',     'php only',     'PHRASE',     'ANY',         'NEGATIVE', -5,  TRUE);
