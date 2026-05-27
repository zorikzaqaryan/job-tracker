package com.zak.jobhunter.telegram;

import com.zak.jobhunter.job.JobPost;
import com.zak.jobhunter.job.JobRuleMatch;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Sends formatted job notifications to a Telegram channel via the Bot API.
 *
 * <p>Uses the {@code sendMessage} method with {@code parse_mode=HTML}.
 * On failure it throws a {@link TelegramSendException} so callers can
 * mark the job as {@code SEND_FAILED}.
 */
@Component
@RequiredArgsConstructor
public class TelegramBotSender {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotSender.class);

    private final TelegramProperties       properties;
    private final TelegramMessageFormatter formatter;
    private final WebClient.Builder        webClientBuilder;

    /**
     * Send a matched job to the configured output channel.
     *
     * @throws TelegramSendException if the Bot API returns an error or the
     *                               bot token / channel ID is not configured.
     */
    public void sendMatchedJob(JobPost job, List<JobRuleMatch> matches) {
        sendMessage(formatter.format(job, matches));
    }

    /**
     * Send a pre-formatted HTML message to the configured output channel.
     * Used by the AI analysis listener for AI-qualified notifications.
     */
    public void sendMessage(String htmlText) {
        String token     = properties.bot().token();
        String channelId = properties.bot().outputChannelId();

        if (token == null || token.isBlank()) {
            throw new TelegramSendException("Telegram bot token is not configured");
        }
        if (channelId == null || channelId.isBlank()) {
            throw new TelegramSendException("Telegram output channel ID is not configured");
        }

        String baseUrl = properties.bot().apiBaseUrl();
        String url     = baseUrl + "/bot" + token + "/sendMessage";

        Map<String, Object> payload = Map.of(
                "chat_id",    channelId,
                "text",       htmlText,
                "parse_mode", "HTML",
                "disable_web_page_preview", false
        );

        try {
            WebClient client = webClientBuilder.build();
            client.post()
                    .uri(url)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Sent message to Telegram channel {}", channelId);
        } catch (WebClientResponseException ex) {
            log.error("Telegram Bot API error: {} — {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new TelegramSendException("Telegram Bot API returned " + ex.getStatusCode(), ex);
        } catch (Exception ex) {
            log.error("Failed to send message to Telegram: {}", ex.getMessage(), ex);
            throw new TelegramSendException("Failed to send message to Telegram", ex);
        }
    }

    public static class TelegramSendException extends RuntimeException {
        public TelegramSendException(String message) { super(message); }
        public TelegramSendException(String message, Throwable cause) { super(message, cause); }
    }
}
