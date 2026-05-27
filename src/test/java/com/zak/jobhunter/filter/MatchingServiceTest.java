package com.zak.jobhunter.filter;

import com.zak.jobhunter.config.AppProperties;
import com.zak.jobhunter.filter.dto.MatchedRuleDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchingServiceTest {

    @Mock private FilterRuleRepository ruleRepository;
    @Mock private AppProperties         appProperties;

    private TextNormalizer  normalizer;
    private MatchingService matchingService;

    @BeforeEach
    void setUp() {
        normalizer      = new TextNormalizer();
        AppProperties.Matching matching = new AppProperties.Matching(7);
        when(appProperties.matching()).thenReturn(matching);
        matchingService = new MatchingService(ruleRepository, normalizer, appProperties);
    }

    // ── WHOLE_WORD ────────────────────────────────────────────────────────

    @Nested
    class WholeWordMatch {

        @Test
        void java_matchesJavaDeveloper() {
            assertThat(matchingService.matches(MatchType.WHOLE_WORD, "java", "java developer")).isTrue();
        }

        @Test
        void java_doesNotMatchJavaScript() {
            assertThat(matchingService.matches(MatchType.WHOLE_WORD, "java", "javascript developer")).isFalse();
        }

        @Test
        void java_doesNotMatchJavaScript_mixedCase() {
            // Text is pre-normalized; this asserts the underlying regex boundary logic
            assertThat(matchingService.matches(MatchType.WHOLE_WORD, "java", "javascript")).isFalse();
        }

        @Test
        void remote_matchesInSentence() {
            assertThat(matchingService.matches(MatchType.WHOLE_WORD, "remote", "senior java developer remote")).isTrue();
        }

        @Test
        void senior_matchesSeniorJava() {
            assertThat(matchingService.matches(MatchType.WHOLE_WORD, "senior", "senior java backend developer")).isTrue();
        }

        @Test
        void lead_doesNotMatchLeadership() {
            assertThat(matchingService.matches(MatchType.WHOLE_WORD, "lead", "leadership skills required")).isFalse();
        }
    }

    // ── PHRASE ────────────────────────────────────────────────────────────

    @Nested
    class PhraseMatch {

        @Test
        void fullRemote_matchesInText() {
            assertThat(matchingService.matches(MatchType.PHRASE, "full remote", "this is a full remote position")).isTrue();
        }

        @Test
        void fullRemote_doesNotMatchPartial() {
            assertThat(matchingService.matches(MatchType.PHRASE, "full remote", "fully remote position")).isFalse();
        }

        @Test
        void springBoot_matchesPhraseInDescription() {
            assertThat(matchingService.matches(MatchType.PHRASE, "spring boot", "experience with spring boot required")).isTrue();
        }

        @Test
        void usOnly_matchesNegativePhrase() {
            assertThat(matchingService.matches(MatchType.PHRASE, "us only", "applicants must be us only")).isTrue();
        }
    }

    // ── CONTAINS ──────────────────────────────────────────────────────────

    @Nested
    class ContainsMatch {

        @Test
        void simpleSubstring() {
            assertThat(matchingService.matches(MatchType.CONTAINS, "java", "java developer")).isTrue();
        }

        @Test
        void partialMatch() {
            // CONTAINS allows partial match (unlike WHOLE_WORD)
            assertThat(matchingService.matches(MatchType.CONTAINS, "java", "javascript")).isTrue();
        }
    }

    // ── REGEX ─────────────────────────────────────────────────────────────

    @Nested
    class RegexMatch {

        @Test
        void simpleRegex() {
            assertThat(matchingService.matches(MatchType.REGEX, "java|kotlin", "kotlin developer")).isTrue();
        }

        @Test
        void invalidRegexReturnsFalse() {
            // Should not throw; invalid regex is handled gracefully
            assertThat(matchingService.matches(MatchType.REGEX, "[invalid(", "any text")).isFalse();
        }
    }

    // ── Full evaluate pipeline ────────────────────────────────────────────

    @Test
    void evaluate_scoresAndCollectsMatchedRules() {
        FilterRule remoteRule = buildRule(1L, "remote", MatchType.WHOLE_WORD, RuleField.ANY, RuleType.POSITIVE, 5);
        FilterRule javaRule   = buildRule(2L, "java",   MatchType.WHOLE_WORD, RuleField.TITLE, RuleType.POSITIVE, 4);
        FilterRule usOnlyRule = buildRule(3L, "us only", MatchType.PHRASE,    RuleField.ANY, RuleType.NEGATIVE, -10);

        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(remoteRule, javaRule, usOnlyRule));

        MatchingService.MatchResult result = matchingService.evaluate(
                "Senior Java Backend Developer",
                "Remote role, open to Armenia and worldwide candidates.",
                "Yerevan / Remote");

        // remote (+5 from description or location) + java (+4 from title) = 9
        assertThat(result.score()).isEqualTo(9);
        assertThat(result.matchedRules()).hasSize(2);
        assertThat(result.isMatched(7)).isTrue();
    }

    @Test
    void evaluate_negativeRuleReducesScore() {
        FilterRule javaRule   = buildRule(1L, "java",    MatchType.WHOLE_WORD, RuleField.ANY, RuleType.POSITIVE, 4);
        FilterRule usOnlyRule = buildRule(2L, "us only", MatchType.PHRASE,     RuleField.ANY, RuleType.NEGATIVE, -10);

        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(javaRule, usOnlyRule));

        MatchingService.MatchResult result = matchingService.evaluate(
                "Java Developer",
                "US only position",
                null);

        assertThat(result.score()).isEqualTo(-6); // 4 - 10
        assertThat(result.isMatched(7)).isFalse();
    }

    @Test
    void evaluate_noMatchReturnsZeroScore() {
        FilterRule rule = buildRule(1L, "kubernetes", MatchType.WHOLE_WORD, RuleField.ANY, RuleType.POSITIVE, 3);
        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(rule));

        MatchingService.MatchResult result = matchingService.evaluate("PHP Developer", "WordPress site", null);
        assertThat(result.score()).isZero();
        assertThat(result.matchedRules()).isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private FilterRule buildRule(Long id, String keyword, MatchType matchType,
                                 RuleField field, RuleType ruleType, int weight) {
        FilterRule rule = new FilterRule();
        rule.setId(id);
        rule.setKeyword(keyword);
        rule.setNormalizedKeyword(normalizer.normalizeKeyword(keyword));
        rule.setMatchType(matchType);
        rule.setField(field);
        rule.setRuleType(ruleType);
        rule.setWeight(weight);
        rule.setEnabled(true);
        return rule;
    }
}
