package com.zak.jobhunter.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class HashUtilsTest {

    @Test
    void sha256_producesExpectedLength() {
        String hash = HashUtils.sha256("hello");
        assertThat(hash).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void sha256_isDeterministic() {
        assertThat(HashUtils.sha256("test input")).isEqualTo(HashUtils.sha256("test input"));
    }

    @Test
    void sha256_differentInputsProduceDifferentHashes() {
        assertThat(HashUtils.sha256("foo")).isNotEqualTo(HashUtils.sha256("bar"));
    }

    @Test
    void sha256_nullInputThrows() {
        assertThatThrownBy(() -> HashUtils.sha256(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sha256_blankInputThrows() {
        assertThatThrownBy(() -> HashUtils.sha256("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rawMessageHash_isDeterministic() {
        String h1 = HashUtils.rawMessageHash("Senior Java Developer. Remote.");
        String h2 = HashUtils.rawMessageHash("Senior Java Developer. Remote.");
        assertThat(h1).isEqualTo(h2);
    }

    @Test
    void rawMessageHash_isCaseInsensitive() {
        // normalize() lowercases, so same text different case → same hash
        String h1 = HashUtils.rawMessageHash("Senior Java Developer");
        String h2 = HashUtils.rawMessageHash("senior java developer");
        assertThat(h1).isEqualTo(h2);
    }

    @Test
    void jobContentHash_usesStructuredFieldsWhenAvailable() {
        String hash1 = HashUtils.jobContentHash("Java Dev", "Acme", "Remote", "https://ex.com", "any raw text");
        String hash2 = HashUtils.jobContentHash("Java Dev", "Acme", "Remote", "https://ex.com", "DIFFERENT raw text");
        // Structured fields match → same hash despite different raw text
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void jobContentHash_fallsBackToRawTextWhenNoStructuredFields() {
        String hash1 = HashUtils.jobContentHash(null, null, null, null, "unique raw text");
        String hash2 = HashUtils.jobContentHash(null, null, null, null, "different raw text");
        assertThat(hash1).isNotEqualTo(hash2);
    }
}
