package com.zak.jobhunter.filter;

public enum MatchType {
    /** Simple substring match on normalized text */
    CONTAINS,
    /** Exact phrase match on normalized text (order-sensitive) */
    PHRASE,
    /** Whole-word match via regex word boundaries — Java matches "Java developer" but not "JavaScript" */
    WHOLE_WORD,
    /** User-supplied regular expression; invalid patterns are caught and logged */
    REGEX
}
