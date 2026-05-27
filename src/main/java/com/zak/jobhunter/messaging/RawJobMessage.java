package com.zak.jobhunter.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * Message published to {@code raw-job-messages} queue.
 * Represents a raw Telegram (or other source) post that has not yet been
 * normalised, deduplicated, or scored.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RawJobMessage implements Serializable {

    private String sourceType;
    private String sourceName;
    private String sourceChannelId;
    private Long   sourceId;

    /** Provider-assigned message ID (e.g. Telegram message ID as string) */
    private String externalMessageId;

    private String rawText;
    private String url;
    private Instant publishedAt;
}
