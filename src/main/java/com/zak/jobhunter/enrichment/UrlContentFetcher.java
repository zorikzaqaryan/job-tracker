package com.zak.jobhunter.enrichment;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;

/**
 * Fetches and parses a URL into a Jsoup {@link Document}.
 *
 * <p>Does NOT:
 * <ul>
 *   <li>Solve CAPTCHAs</li>
 *   <li>Log full page content (only summaries)</li>
 *   <li>Follow redirects beyond 3 hops</li>
 *   <li>Deep-crawl nested pages (MVP: original URL only)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class UrlContentFetcher {

    private static final Logger log = LoggerFactory.getLogger(UrlContentFetcher.class);
    private static final String USER_AGENT =
            "Mozilla/5.0 (compatible; JobHunterBot/1.0; +https://github.com/zorikz/job-hunter)";

    private final UrlEnrichmentProperties properties;

    /**
     * Result of a fetch attempt.
     *
     * @param document  parsed HTML document, or null on failure
     * @param httpStatus HTTP status code, or null on network error
     * @param error     error message, or null on success
     * @param status    fetch status enum
     */
    public record FetchResult(
            Document document,
            Integer httpStatus,
            String error,
            UrlEnrichmentStatus status
    ) {
        static FetchResult success(Document doc, int httpStatus) {
            return new FetchResult(doc, httpStatus, null, UrlEnrichmentStatus.SUCCESS);
        }
        static FetchResult timeout(String msg) {
            return new FetchResult(null, null, msg, UrlEnrichmentStatus.TIMEOUT);
        }
        static FetchResult failed(String msg, Integer httpStatus) {
            return new FetchResult(null, httpStatus, msg, UrlEnrichmentStatus.FAILED);
        }
    }

    public FetchResult fetch(String url) {
        if (url == null || url.isBlank()) {
            return FetchResult.failed("URL is blank", null);
        }

        // Validate URL format
        try {
            URI.create(url);
        } catch (IllegalArgumentException e) {
            return FetchResult.failed("Invalid URL: " + url, null);
        }

        int timeoutMs = properties.timeoutSeconds() * 1_000;

        try {
            log.debug("[URL-FETCH] Fetching: {}", url);
            org.jsoup.Connection.Response response = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(timeoutMs)
                    .maxBodySize(5 * 1024 * 1024)   // 5 MB max
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .execute();

            int status = response.statusCode();
            if (status >= 400) {
                log.warn("[URL-FETCH] HTTP {} for URL: {}", status, url);
                return FetchResult.failed("HTTP " + status, status);
            }

            Document doc = response.parse();
            log.info("[URL-FETCH] ✔ Fetched  url={} status={}", url, status);
            return FetchResult.success(doc, status);

        } catch (SocketTimeoutException ex) {
            log.warn("[URL-FETCH] Timeout fetching URL: {}", url);
            return FetchResult.timeout("Timed out after " + properties.timeoutSeconds() + "s");
        } catch (IOException ex) {
            log.warn("[URL-FETCH] IO error for URL {}: {}", url, ex.getMessage());
            return FetchResult.failed("IO error: " + ex.getMessage(), null);
        }
    }
}
