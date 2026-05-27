package com.zak.jobhunter.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Matching matching
) {
    public record Matching(@Min(0) int threshold) {}
}
