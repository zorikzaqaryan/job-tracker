package com.zak.jobhunter.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class HashUtils {

    private HashUtils() {}

    /**
     * Computes a SHA-256 hex digest from the given input string.
     * Returns a 64-character lowercase hex string.
     */
    public static String sha256(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Cannot hash null or blank input");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Builds a canonical content hash for a job post from its stable fields.
     * Falls back to rawText if all structured fields are blank.
     */
    public static String jobContentHash(String title, String company, String location, String url, String rawText) {
        String canonical = normalize(title) + "|" + normalize(company) + "|"
                + normalize(location) + "|" + normalize(url);
        if (canonical.equals("|||")) {
            canonical = normalize(rawText);
        }
        return sha256(canonical.isBlank() ? rawText : canonical);
    }

    /**
     * Builds the deduplication hash for a raw message based on its raw text.
     */
    public static String rawMessageHash(String rawText) {
        return sha256(normalize(rawText));
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}
