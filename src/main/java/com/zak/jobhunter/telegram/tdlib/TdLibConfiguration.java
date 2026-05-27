package com.zak.jobhunter.telegram.tdlib;

import it.tdlight.Init;
import it.tdlight.Log;
import it.tdlight.Slf4JLogMessageHandler;
import it.tdlight.client.SimpleTelegramClientFactory;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Loads TDLight native libraries and exposes a single {@link SimpleTelegramClientFactory}.
 * Only one factory instance is allowed per JVM (TDLight requirement).
 */
@Configuration
@ConditionalOnProperty(name = "app.telegram.user.enabled", havingValue = "true")
public class TdLibConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TdLibConfiguration.class);

    private SimpleTelegramClientFactory clientFactory;

    @jakarta.annotation.PostConstruct
    void initNativeLibraries() {
        try {
            Init.init();
            Log.setLogMessageHandler(1, new Slf4JLogMessageHandler());
            log.info("TDLight native libraries initialized");
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to load TDLight native libraries. See TDLIB_SETUP.md — install VC++ Redistributable "
                            + "and ensure tdlight-natives windows_amd64 is on the classpath.", ex);
        }
    }

    @Bean
    SimpleTelegramClientFactory tdLightClientFactory() {
        clientFactory = new SimpleTelegramClientFactory();
        return clientFactory;
    }

    @PreDestroy
    void shutdown() {
        if (clientFactory != null) {
            try {
                clientFactory.close();
                log.info("TDLight client factory closed");
            } catch (Exception ex) {
                log.warn("Error closing TDLight factory: {}", ex.getMessage());
            }
        }
    }
}
