-- V3: Russian-language and EMEA/Eastern-Europe filter rules
-- NOTE: This file must be saved and read as UTF-8.

INSERT INTO filter_rules (keyword, normalized_keyword, match_type, field, rule_type, weight, enabled)
VALUES
    -- ── Armenian/Russian location keywords ───────────────────────────────────
    ('Ереван',              'ереван',              'WHOLE_WORD', 'ANY', 'POSITIVE', 5, TRUE),
    ('Армения',             'армения',             'WHOLE_WORD', 'ANY', 'POSITIVE', 5, TRUE),

    -- ── Russian remote variants ───────────────────────────────────────────────
    -- е with combining accent (ё) and plain e variant both covered
    ('удалёнка',           'удалёнка',            'WHOLE_WORD', 'ANY', 'POSITIVE', 4, TRUE),
    ('удаленка',           'удаленка',            'WHOLE_WORD', 'ANY', 'POSITIVE', 4, TRUE),
    ('Удалённая работа',   'удалённая работа',    'PHRASE',     'ANY', 'POSITIVE', 5, TRUE),
    ('Удаленная работа',   'удаленная работа',    'PHRASE',     'ANY', 'POSITIVE', 5, TRUE),
    ('Можно удалённо',     'можно удалённо',      'PHRASE',     'ANY', 'POSITIVE', 5, TRUE),
    ('Можно удаленно',     'можно удаленно',      'PHRASE',     'ANY', 'POSITIVE', 5, TRUE),
    ('работа на дому',     'работа на дому',      'PHRASE',     'ANY', 'POSITIVE', 4, TRUE),
    ('100% удалённо',      '100% удалённо',       'PHRASE',     'ANY', 'POSITIVE', 6, TRUE),
    ('100% удаленно',      '100% удаленно',       'PHRASE',     'ANY', 'POSITIVE', 6, TRUE),
    ('формат работы: удалённый', 'формат работы: удалённый', 'PHRASE', 'ANY', 'POSITIVE', 5, TRUE),
    ('формат работы: удаленный', 'формат работы: удаленный', 'PHRASE', 'ANY', 'POSITIVE', 5, TRUE),

    -- ── EMEA / Eastern Europe remote ─────────────────────────────────────────
    ('EMEA remote',              'emea remote',               'PHRASE', 'ANY', 'POSITIVE', 5, TRUE),
    ('Eastern Europe remote',    'eastern europe remote',     'PHRASE', 'ANY', 'POSITIVE', 5, TRUE);
