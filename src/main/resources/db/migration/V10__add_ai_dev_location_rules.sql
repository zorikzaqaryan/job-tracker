-- Broader first-stage matching; AI qualification is the second gate.

INSERT INTO filter_rules (keyword, normalized_keyword, match_type, field, rule_type, weight, enabled)
SELECT 'Worldwide (location)', 'worldwide', 'WHOLE_WORD', 'LOCATION', 'POSITIVE', 4, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM filter_rules WHERE normalized_keyword = 'worldwide' AND field = 'LOCATION'
);

INSERT INTO filter_rules (keyword, normalized_keyword, match_type, field, rule_type, weight, enabled)
SELECT 'World Wide (location)', 'world wide', 'PHRASE', 'LOCATION', 'POSITIVE', 4, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM filter_rules WHERE normalized_keyword = 'world wide' AND field = 'LOCATION'
);

INSERT INTO filter_rules (keyword, normalized_keyword, match_type, field, rule_type, weight, enabled)
SELECT 'Developer', 'developer', 'WHOLE_WORD', 'ANY', 'POSITIVE', 3, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM filter_rules WHERE normalized_keyword = 'developer' AND field = 'ANY'
);

INSERT INTO filter_rules (keyword, normalized_keyword, match_type, field, rule_type, weight, enabled)
SELECT 'Dev', 'dev', 'WHOLE_WORD', 'TITLE', 'POSITIVE', 2, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM filter_rules WHERE normalized_keyword = 'dev' AND field = 'TITLE'
);

INSERT INTO filter_rules (keyword, normalized_keyword, match_type, field, rule_type, weight, enabled)
SELECT 'AI', 'ai', 'WHOLE_WORD', 'ANY', 'POSITIVE', 3, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM filter_rules WHERE normalized_keyword = 'ai' AND field = 'ANY'
);
