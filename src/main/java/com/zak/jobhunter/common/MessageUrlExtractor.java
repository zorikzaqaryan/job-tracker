package com.zak.jobhunter.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts job application URLs from Telegram post text.
 */
public final class MessageUrlExtractor {

    private static final Pattern APPLY_LINE = Pattern.compile(
            "(?i)apply\\s*[→>\\-–]\\s*(https?://\\S+)",
            Pattern.UNICODE_CHARACTER_CLASS);

    private static final Pattern ANY_URL = Pattern.compile(
            "https?://[^\\s<>\"']+",
            Pattern.CASE_INSENSITIVE);

    private MessageUrlExtractor() {}

    /**
     * Prefer URL on an "Apply → …" line; otherwise first non-Telegram http(s) link.
     */
    public static String extractApplyUrl(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher apply = APPLY_LINE.matcher(text);
        if (apply.find()) {
            String url = trimTrailingPunctuation(apply.group(1));
            if (!isTelegramUrl(url)) {
                return url;
            }
        }
        Matcher urls = ANY_URL.matcher(text);
        while (urls.find()) {
            String url = trimTrailingPunctuation(urls.group());
            if (!isTelegramUrl(url)) {
                return url;
            }
        }
        return null;
    }

    private static String trimTrailingPunctuation(String url) {
        if (url == null) {
            return null;
        }
        return url.replaceAll("[.,;:!?)\\]]+$", "");
    }

    private static boolean isTelegramUrl(String url) {
        if (url == null) {
            return true;
        }
        String lower = url.toLowerCase();
        return lower.contains("t.me/") || lower.contains("telegram.me/");
    }
}
