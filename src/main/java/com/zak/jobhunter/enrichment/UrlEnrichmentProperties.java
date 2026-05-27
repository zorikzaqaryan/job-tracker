package com.zak.jobhunter.enrichment;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * URL enrichment configuration bound to {@code app.url-enrichment.*}.
 */
@ConfigurationProperties(prefix = "app.url-enrichment")
public record UrlEnrichmentProperties(
        boolean enabled,
        int maxNestedLinks,
        int timeoutSeconds,
        int maxPageChars,
        boolean allowedDomainsOnly
) {}
