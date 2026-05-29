package com.zak.jobhunter.ingestion;

import com.zak.jobhunter.common.HashUtils;
import com.zak.jobhunter.common.MessageUrlExtractor;
import com.zak.jobhunter.job.JobPost;
import com.zak.jobhunter.job.JobService;
import com.zak.jobhunter.messaging.RabbitQueues;
import com.zak.jobhunter.messaging.RawJobMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Consumes messages from {@code raw-job-messages} and runs the full
 * processing pipeline: persist → deduplicate → normalize → match → (send).
 *
 * <p>Spring AMQP will retry failed messages up to the configured max-attempts,
 * then route them to {@code raw-job-messages.dlq} via the dead-letter exchange.
 */
@Component
@RequiredArgsConstructor
public class RawMessageListener {

    private static final Logger log = LoggerFactory.getLogger(RawMessageListener.class);

    private final RawMessageRepository rawMessageRepository;
    private final RawMessageService    rawMessageService;
    private final JobService           jobService;

    @RabbitListener(queues = RabbitQueues.RAW_QUEUE)
    @Transactional
    public void onRawJobMessage(RawJobMessage message) {
        if (message == null || message.getRawText() == null || message.getRawText().isBlank()) {
            log.warn("[QUEUE] Received blank/null message — discarding");
            return;
        }

        String channel   = message.getSourceName() != null ? message.getSourceName() : message.getSourceChannelId();
        String msgId     = message.getExternalMessageId();
        String textSnip  = snippet(message.getRawText(), 80);

        log.info("[QUEUE] ▶ Incoming  channel='{}' msgId={} text='{}'", channel, msgId, textSnip);

        // 1. Find or fetch the persisted RawMessage
        Optional<RawMessage> existing = rawMessageRepository
                .findBySourceTypeAndSourceChannelIdAndExternalMessageId(
                        message.getSourceType(),
                        message.getSourceChannelId(),
                        message.getExternalMessageId());

        RawMessage rawMessage;
        if (existing.isPresent()) {
            rawMessage = existing.get();
            if (rawMessage.isProcessed()) {
                log.info("[QUEUE] ⏭  Already processed — skipping  channel='{}' msgId={} rawId={}",
                        channel, msgId, rawMessage.getId());
                return;
            }
            log.debug("[QUEUE] Found existing unprocessed rawMessage id={}", rawMessage.getId());
        } else {
            String contentHash = HashUtils.rawMessageHash(message.getRawText());

            if (rawMessageRepository.existsByContentHash(contentHash)) {
                log.info("[QUEUE] ♻  Duplicate content — skipping  channel='{}' msgId={} hash={}",
                        channel, msgId, contentHash.substring(0, 12) + "...");
                return;
            }

            rawMessage = RawMessage.builder()
                    .sourceType(message.getSourceType() != null ? message.getSourceType() : "UNKNOWN")
                    .sourceName(message.getSourceName())
                    .sourceChannelId(message.getSourceChannelId())
                    .externalMessageId(message.getExternalMessageId() != null
                            ? message.getExternalMessageId()
                            : String.valueOf(System.currentTimeMillis()))
                    .rawText(message.getRawText())
                    .url(message.getUrl() != null && !message.getUrl().isBlank()
                            ? message.getUrl()
                            : MessageUrlExtractor.extractApplyUrl(message.getRawText()))
                    .publishedAt(message.getPublishedAt())
                    .contentHash(contentHash)
                    .processed(false)
                    .build();
            rawMessage = rawMessageRepository.save(rawMessage);
            log.debug("[QUEUE] Persisted new rawMessage id={}", rawMessage.getId());
        }

        // 2. Process through the job pipeline
        try {
            JobPost job = jobService.processRawMessage(rawMessage);
            rawMessageService.markProcessed(rawMessage.getId());
            log.info("[QUEUE] ✔ Pipeline done  channel='{}' msgId={} → jobId={} score={} status={}",
                    channel, msgId, job.getId(), job.getScore(), job.getStatus());
        } catch (Exception ex) {
            log.error("[QUEUE] ✘ Pipeline failed  channel='{}' msgId={} rawId={} error={}",
                    channel, msgId, rawMessage.getId(), ex.getMessage(), ex);
            throw ex;
        }
    }

    private static String snippet(String text, int maxLen) {
        if (text == null) return "";
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= maxLen ? oneLine : oneLine.substring(0, maxLen) + "…";
    }
}
