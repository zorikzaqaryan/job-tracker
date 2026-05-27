package com.zak.jobhunter.telegram;

import com.zak.jobhunter.channel.ChannelService;
import com.zak.jobhunter.channel.SourceType;
import com.zak.jobhunter.channel.dto.ChannelResponse;
import com.zak.jobhunter.messaging.RabbitQueues;
import com.zak.jobhunter.messaging.RawJobMessage;
import com.zak.jobhunter.telegram.tdlib.TdLibChannelRegistry;
import it.tdlight.client.APIToken;
import it.tdlight.client.AuthenticationSupplier;
import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.client.SimpleTelegramClientBuilder;
import it.tdlight.client.SimpleTelegramClientFactory;
import it.tdlight.client.TDLibSettings;
import it.tdlight.jni.TdApi;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TDLight-based Telegram user account collector.
 *
 * <p>Reads messages from channels you are subscribed to with your personal account
 * and publishes them to {@code raw-job-messages} for the normal pipeline.
 *
 * <p>Setup guide: {@code TDLIB_SETUP.md}
 */
@Component
@ConditionalOnProperty(name = "app.telegram.user.enabled", havingValue = "true")
@RequiredArgsConstructor
public class TdLibTelegramClient implements TelegramUserCollector {

    private static final Logger log = LoggerFactory.getLogger(TdLibTelegramClient.class);

    private final TelegramProperties       properties;
    private final ChannelService           channelService;
    private final RabbitTemplate           rabbitTemplate;
    private final SimpleTelegramClientFactory clientFactory;
    private final TdLibChannelRegistry     channelRegistry;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile SimpleTelegramClient client;

    /** TDLib API calls must not block on the TDLib update thread — use this executor. */
    private final ExecutorService tdlibApiExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "tdlib-api-worker");
        t.setDaemon(true);
        return t;
    });

    @Override
    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        Thread connector = new Thread(this::connect, "tdlib-connector");
        connector.setDaemon(true);
        connector.start();
    }

    @Override
    @PreDestroy
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        shutdownTdLib();
        tdlibApiExecutor.shutdown();
        try {
            if (!tdlibApiExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                tdlibApiExecutor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            tdlibApiExecutor.shutdownNow();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private void connect() {
        try {
            validateConfig();

            APIToken apiToken = new APIToken(
                    properties.user().apiId(),
                    properties.user().apiHash());

            TDLibSettings settings = TDLibSettings.create(apiToken);
            SessionPaths session = resolveSessionPaths();
            settings.setDatabaseDirectoryPath(session.database());
            settings.setDownloadedFilesDirectoryPath(session.files());

            log.info("[TDLib] Session directory: {} (reuse after first login — no OTP/2FA on restart)",
                    session.database());

            SimpleTelegramClientBuilder builder = clientFactory.builder(settings);
            builder.addUpdateHandler(TdApi.UpdateAuthorizationState.class, this::onAuthorizationState);
            builder.addUpdateHandler(TdApi.UpdateNewMessage.class, this::onNewMessage);

            log.info("[TDLib] Connecting as user (phone configured: {}) …",
                    properties.user().phoneNumber() != null && !properties.user().phoneNumber().isBlank());

            client = buildClient(builder);
            log.info("[TDLib] Client started. If first login, check the console for OTP / password prompts.");
        } catch (Exception ex) {
            running.set(false);
            log.error("[TDLib] Failed to start collector: {}", ex.getMessage(), ex);
        }
    }

    private SimpleTelegramClient buildClient(SimpleTelegramClientBuilder builder) {
        String phone = properties.user().phoneNumber();
        if (phone != null && !phone.isBlank()) {
            return builder.build(AuthenticationSupplier.user(phone.trim()));
        }
        log.warn("[TDLib] TELEGRAM_PHONE_NUMBER not set — using interactive console login");
        return builder.build(AuthenticationSupplier.consoleLogin());
    }

    /**
     * Per-phone session folder so TDLib can reuse auth after the first successful login.
     */
    private SessionPaths resolveSessionPaths() {
        String baseDb = properties.user().databaseDirectory();
        String baseFiles = properties.user().filesDirectory();
        String phone = properties.user().phoneNumber();
        String suffix = "default";
        if (phone != null && !phone.isBlank()) {
            suffix = phone.trim().replaceAll("[^0-9+]", "").replace("+", "");
        }
        return new SessionPaths(
                Paths.get(baseDb, "session-" + suffix).toAbsolutePath(),
                Paths.get(baseFiles, "session-" + suffix).toAbsolutePath());
    }

    private record SessionPaths(Path database, Path files) {}

    private void validateConfig() {
        if (properties.user().apiId() <= 0) {
            throw new IllegalStateException(
                    "TELEGRAM_API_ID is missing or invalid. Get it from https://my.telegram.org/apps");
        }
        if (properties.user().apiHash() == null || properties.user().apiHash().isBlank()) {
            throw new IllegalStateException(
                    "TELEGRAM_API_HASH is missing. Get it from https://my.telegram.org/apps");
        }
    }

    private void onAuthorizationState(TdApi.UpdateAuthorizationState update) {
        TdApi.AuthorizationState state = update.authorizationState;
        if (state instanceof TdApi.AuthorizationStateWaitPhoneNumber) {
            log.info("[TDLib] Waiting for phone number (should be sent automatically from config)");
        } else if (state instanceof TdApi.AuthorizationStateWaitCode) {
            log.info("[TDLib] *** Enter the login code in this console (Telegram app → message from Telegram) ***");
        } else if (state instanceof TdApi.AuthorizationStateWaitPassword waitPassword) {
            log.info("[TDLib] *** Enter your Telegram Cloud Password (2FA) in this console ***");
            log.info("[TDLib] This is the password you set in Telegram → Settings → Privacy → Two-Step Verification.");
            if (waitPassword.passwordHint != null && !waitPassword.passwordHint.isBlank()) {
                log.info("[TDLib] Hint from Telegram: '{}' — reminder only, NOT your password",
                        waitPassword.passwordHint);
            }
        } else if (state instanceof TdApi.AuthorizationStateReady) {
            log.info("[TDLib] Logged in successfully — session saved; future restarts should not ask for code/password");
            // Must not call blocking TDLib APIs on this thread (deadlock → 60s timeout)
            scheduleChannelRefresh();
        } else if (state instanceof TdApi.AuthorizationStateClosed) {
            log.warn("[TDLib] Session closed");
            running.set(false);
        }
    }

    private void scheduleChannelRefresh() {
        tdlibApiExecutor.execute(() -> {
            if (client == null || !running.get()) {
                return;
            }
            try {
                log.info("[TDLib] Resolving tracked channels on worker thread …");
                var sources = channelService.findAll(SourceType.TELEGRAM, true);
                channelRegistry.refresh(client, sources);
            } catch (Exception ex) {
                log.error("[TDLib] Channel refresh failed: {}", describeError(ex), ex);
            }
        });
    }

    private static String describeError(Throwable ex) {
        if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
            return ex.getMessage();
        }
        return ex.getClass().getSimpleName()
                + (ex.getCause() != null ? " — " + describeError(ex.getCause()) : "");
    }

    private void onNewMessage(TdApi.UpdateNewMessage update) {
        if (client == null || !running.get()) {
            return;
        }

        TdApi.Message message = update.message;
        long chatId = message.chatId;

        Optional<ChannelResponse> sourceOpt = channelRegistry.findByChatId(chatId);
        if (sourceOpt.isEmpty()) {
            return;
        }

        String text = extractText(message);
        if (text == null || text.isBlank()) {
            return;
        }

        ChannelResponse source = sourceOpt.get();
        String snippet = text.length() > 80 ? text.substring(0, 80) + "…" : text;

        log.info("[TDLib] ▶ Message from channel='{}' chatId={} msgId={} text='{}'",
                source.name(), chatId, message.id, snippet);

        String channelIdForDb = source.telegramChannelId() != null && !source.telegramChannelId().isBlank()
                ? source.telegramChannelId()
                : String.valueOf(chatId);

        RawJobMessage queueMessage = RawJobMessage.builder()
                .sourceType(SourceType.TELEGRAM.name())
                .sourceName(source.name())
                .sourceChannelId(channelIdForDb)
                .sourceId(source.id())
                .externalMessageId(String.valueOf(message.id))
                .rawText(text)
                .publishedAt(Instant.ofEpochSecond(message.date))
                .build();

        rabbitTemplate.convertAndSend(
                RabbitQueues.EXCHANGE,
                RabbitQueues.RAW_ROUTING_KEY,
                queueMessage);

        log.debug("[TDLib] Published to {} queue  channel='{}' msgId={}",
                RabbitQueues.RAW_QUEUE, source.name(), message.id);
    }

    private static String extractText(TdApi.Message message) {
        if (message.content instanceof TdApi.MessageText messageText) {
            return messageText.text.text;
        }
        return null;
    }

    private void shutdownTdLib() {
        if (client != null) {
            try {
                client.sendClose();
                log.info("[TDLib] Collector shut down");
            } catch (Exception ex) {
                log.warn("[TDLib] Error during shutdown: {}", ex.getMessage());
            } finally {
                client = null;
            }
        }
    }
}
