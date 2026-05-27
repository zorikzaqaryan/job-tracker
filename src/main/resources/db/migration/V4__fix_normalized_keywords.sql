-- V4: Fix normalized_keyword values where punctuation (%, :) was left in place.
-- The TextNormalizer strips punctuation before matching, so normalized_keyword
-- must also have punctuation stripped to ensure contains() comparisons succeed.

UPDATE filter_rules SET normalized_keyword = '100 удалённо'  WHERE keyword = '100% удалённо';
UPDATE filter_rules SET normalized_keyword = '100 удаленно'  WHERE keyword = '100% удаленно';
UPDATE filter_rules SET normalized_keyword = 'формат работы  удалённый' WHERE keyword = 'формат работы: удалённый';
UPDATE filter_rules SET normalized_keyword = 'формат работы  удаленный' WHERE keyword = 'формат работы: удаленный';
