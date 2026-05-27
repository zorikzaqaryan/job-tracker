package com.zak.jobhunter.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class TextNormalizerTest {

    private TextNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new TextNormalizer();
    }

    @Test
    void normalize_lowercasesInput() {
        assertThat(normalizer.normalize("SENIOR JAVA DEVELOPER")).contains("senior java developer");
    }

    @Test
    void normalize_trimsWhitespace() {
        assertThat(normalizer.normalize("  hello  ")).isEqualTo("hello");
    }

    @Test
    void normalize_collapseMultipleSpaces() {
        assertThat(normalizer.normalize("a  b   c")).isEqualTo("a b c");
    }

    @Test
    void normalize_replacePunctuationWithSpace() {
        // Commas and dots → spaces, then consecutive spaces are collapsed
        assertThat(normalizer.normalize("Java, Spring.Boot")).isEqualTo("java spring boot");
    }

    @Test
    void normalize_nullReturnsEmpty() {
        assertThat(normalizer.normalize(null)).isEqualTo("");
    }

    @Test
    void normalize_blankReturnsEmpty() {
        assertThat(normalizer.normalize("   ")).isEqualTo("");
    }

    @ParameterizedTest(name = "variant ''{0}'' → ''{1}''")
    @CsvSource({
            "Fully Remote,      full remote",
            "fully remote,      full remote",
            "World Wide,        worldwide",
            "world wide,        worldwide",
            "Hybrid/Remote,     hybrid remote",
            "hybrid-remote,     hybrid remote",
    })
    void normalize_semanticVariants(String input, String expected) {
        assertThat(normalizer.normalize(input)).isEqualTo(expected.trim());
    }

    @Test
    void normalizeKeyword_doesNotApplyVariants() {
        // normalizeKeyword is for storing the rule; variants are applied on the text side
        assertThat(normalizer.normalizeKeyword("Full Remote")).isEqualTo("full remote");
        assertThat(normalizer.normalizeKeyword("Fully Remote")).isEqualTo("fully remote");
    }
}
