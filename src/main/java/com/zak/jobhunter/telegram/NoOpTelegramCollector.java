package com.zak.jobhunter.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * No-op implementation of {@link TelegramUserCollector} used when TDLib is
 * not installed or {@code app.telegram.user.enabled=false}.
 *
 * <p>This prevents startup failures while the TDLib native library is being
 * set up.  Raw messages can still be ingested via the REST API or directly
 * via RabbitMQ.
 */
@Component
@ConditionalOnProperty(name = "app.telegram.user.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpTelegramCollector implements TelegramUserCollector {

    private static final Logger log = LoggerFactory.getLogger(NoOpTelegramCollector.class);

    @Override
    public void start() {
        log.info("TelegramUserCollector is disabled (app.telegram.user.enabled=false). " +
                 "Use the REST API or configure TDLib to ingest messages.");
    }

    @Override
    public void stop() {
        // nothing to stop
    }

    @Override
    public boolean isRunning() {
        return false;
    }
}
