package com.zak.jobhunter.filter;

import com.zak.jobhunter.config.AppProperties;
import com.zak.jobhunter.filter.dto.MatchedRuleDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Core matching engine.
 *
 * <p>Given a set of job fields (title, description, location) it evaluates all
 * enabled {@link FilterRule}s, accumulates a score, and returns the list of
 * matched rule DTOs together with the total score.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    private final FilterRuleRepository ruleRepository;
    private final TextNormalizer normalizer;
    private final AppProperties appProperties;

    public record MatchResult(int score, List<MatchedRuleDto> matchedRules) {
        public boolean isMatched(int threshold) {
            return score >= threshold;
        }
    }

    /**
     * Evaluate all enabled rules against the supplied job fields.
     */
    public MatchResult evaluate(String title, String description, String location) {
        List<FilterRule> rules = ruleRepository.findByEnabledTrue();

        String normTitle       = normalizer.normalize(title);
        String normDescription = normalizer.normalize(description);
        String normLocation    = normalizer.normalize(location);

        int totalScore = 0;
        List<MatchedRuleDto> matched = new ArrayList<>();

        for (FilterRule rule : rules) {
            String normKeyword = rule.getNormalizedKeyword();

            List<FieldCandidate> fieldsToCheck = resolveFields(rule.getField(), normTitle, normDescription, normLocation);

            for (FieldCandidate candidate : fieldsToCheck) {
                if (matches(rule.getMatchType(), normKeyword, candidate.text())) {
                    totalScore += rule.getWeight();
                    matched.add(new MatchedRuleDto(
                            rule.getId(),
                            rule.getKeyword(),
                            rule.getField(),
                            candidate.fieldName(),
                            extractSnippet(candidate.text(), normKeyword),
                            rule.getWeight()));
                    break; // count each rule at most once
                }
            }
        }

        return new MatchResult(totalScore, matched);
    }

    public int getThreshold() {
        return appProperties.matching().threshold();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private record FieldCandidate(String fieldName, String text) {}

    private List<FieldCandidate> resolveFields(RuleField field,
                                               String title, String description, String location) {
        return switch (field) {
            case TITLE       -> title.isBlank()       ? List.of() : List.of(new FieldCandidate("TITLE", title));
            case DESCRIPTION -> description.isBlank() ? List.of() : List.of(new FieldCandidate("DESCRIPTION", description));
            case LOCATION    -> location.isBlank()    ? List.of() : List.of(new FieldCandidate("LOCATION", location));
            case ANY -> {
                List<FieldCandidate> all = new ArrayList<>();
                if (!title.isBlank())       all.add(new FieldCandidate("TITLE", title));
                if (!description.isBlank()) all.add(new FieldCandidate("DESCRIPTION", description));
                if (!location.isBlank())    all.add(new FieldCandidate("LOCATION", location));
                yield all;
            }
        };
    }

    /**
     * Core matching logic per match type.
     */
    boolean matches(MatchType matchType, String normalizedKeyword, String normalizedText) {
        if (normalizedKeyword.isBlank() || normalizedText.isBlank()) return false;
        return switch (matchType) {
            case CONTAINS   -> normalizedText.contains(normalizedKeyword);
            case PHRASE     -> normalizedText.contains(normalizedKeyword);
            case WHOLE_WORD -> wholeWordMatch(normalizedKeyword, normalizedText);
            case REGEX      -> regexMatch(normalizedKeyword, normalizedText);
        };
    }

    private boolean wholeWordMatch(String keyword, String text) {
        // Build a regex that requires the keyword to be surrounded by word boundaries.
        // We quote the keyword so any regex-special chars in it are treated as literals.
        String pattern = "(?<![\\w-])" + Pattern.quote(keyword) + "(?![\\w-])";
        try {
            return Pattern.compile(pattern).matcher(text).find();
        } catch (PatternSyntaxException e) {
            log.warn("Invalid whole-word pattern for keyword '{}': {}", keyword, e.getMessage());
            return false;
        }
    }

    private boolean regexMatch(String pattern, String text) {
        try {
            return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text).find();
        } catch (PatternSyntaxException e) {
            log.warn("Invalid REGEX rule pattern '{}': {}", pattern, e.getMessage());
            return false;
        }
    }

    private String extractSnippet(String text, String keyword) {
        int idx = text.indexOf(keyword);
        if (idx < 0) return null;
        int start = Math.max(0, idx - 20);
        int end   = Math.min(text.length(), idx + keyword.length() + 20);
        return "..." + text.substring(start, end) + "...";
    }
}
