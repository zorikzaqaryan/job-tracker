package com.zak.jobhunter.filter;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Normalizes free-text for consistent keyword matching.
 * All operations are null-safe and return an empty string for null input.
 */
@Component
public class TextNormalizer {

    private static final Pattern PUNCTUATION = Pattern.compile("[\\p{Punct}&&[^-]]");
    private static final Pattern MULTI_SPACE  = Pattern.compile("\\s+");

    /**
     * Full normalization pipeline:
     * 1. Lowercase
     * 2. Unicode NFC normalization (removes diacritics from composed forms)
     * 3. Strip punctuation (keeping hyphens as word separators)
     * 4. Apply semantic variant substitutions
     * 5. Collapse multiple whitespace into single space
     * 6. Trim
     */
    public String normalize(String text) {
        if (text == null || text.isBlank()) return "";
        String s = text.toLowerCase();
        s = Normalizer.normalize(s, Normalizer.Form.NFC);
        s = PUNCTUATION.matcher(s).replaceAll(" ");
        s = applyVariants(s);
        s = MULTI_SPACE.matcher(s).replaceAll(" ");
        return s.trim();
    }

    /**
     * Expand semantic variants so a single keyword matches multiple phrasings.
     * This runs BEFORE the final collapse so substitutions may themselves
     * introduce spaces that get collapsed.
     */
    private String applyVariants(String s) {
        // "fully remote" → "full remote"
        s = s.replace("fully remote", "full remote");
        // "world wide" ↔ "worldwide" — normalise to "worldwide"
        s = s.replace("world wide", "worldwide");
        // "hybrid/remote" — preserve the "remote" token for matching
        s = s.replace("hybrid/remote", "hybrid remote");
        s = s.replace("hybrid-remote", "hybrid remote");
        return s;
    }

    /**
     * Keyword-level normalizer: used when storing filter rules.
     * Same pipeline, but does NOT apply variant substitutions so the stored
     * keyword stays human-readable and the substitution runs on the target text.
     */
    public String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return "";
        String s = keyword.toLowerCase();
        s = Normalizer.normalize(s, Normalizer.Form.NFC);
        s = PUNCTUATION.matcher(s).replaceAll(" ");
        s = MULTI_SPACE.matcher(s).replaceAll(" ");
        return s.trim();
    }
}
