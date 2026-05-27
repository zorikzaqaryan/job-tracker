package com.zak.jobhunter.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.telegram")
public record TelegramProperties(
        Bot  bot,
        User user
) {
    public record Bot(
            String token,
            String outputChannelId,
            String apiBaseUrl
    ) {}

    public record User(
            int    apiId,
            String apiHash,
            String phoneNumber,
            String databaseDirectory,
            String filesDirectory,
            boolean enabled
    ) {}
}
