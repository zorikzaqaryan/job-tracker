package com.zak.jobhunter.ingestion;

import com.zak.jobhunter.channel.JobSource;
import com.zak.jobhunter.channel.JobSourceRepository;
import com.zak.jobhunter.common.HashUtils;
import com.zak.jobhunter.ingestion.dto.RawMessageDto;
import com.zak.jobhunter.messaging.RabbitQueues;
import com.zak.jobhunter.messaging.RawJobMessage;
import com.zak.jobhunter.metrics.JobHunterMetrics;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RawMessageService {

    private static final Logger log = LoggerFactory.getLogger(RawMessageService.class);

    private final RawMessageRepository rawMessageRepository;
    private final JobSourceRepository  jobSourceRepository;
    private final RabbitTemplate       rabbitTemplate;
    private final JobHunterMetrics     metrics;

    /**
     * Accept a raw message from any source.
     * <ol>
     *   <li>Check for duplicate by (sourceType, sourceChannelId, externalMessageId).</li>
     *   <li>Compute content hash for secondary deduplication.</li>
     *   <li>Persist the raw message.</li>
     *   <li>Publish to RabbitMQ for async processing.</li>
     * </ol>
     *
     * @return the persisted {@link RawMessage}, or the existing one if duplicate.
     */
    @Transactional
    public RawMessage ingest(RawMessageDto dto) {
        // Primary deduplication: exact source + message identity
        Optional<RawMessage> existing = rawMessageRepository
                .findBySourceTypeAndSourceChannelIdAndExternalMessageId(
                        dto.sourceType(), dto.sourceChannelId(), dto.externalMessageId());

        if (existing.isPresent()) {
            log.info("[INGEST] ♻ Duplicate msgId — skipping  channel='{}' msgId={}",
                    dto.sourceName(), dto.externalMessageId());
            metrics.recordDuplicate(dto.sourceType(), dto.sourceName(), "msgid");
            return existing.get();
        }

        String contentHash = HashUtils.rawMessageHash(dto.text());

        if (rawMessageRepository.existsByContentHash(contentHash)) {
            log.info("[INGEST] ♻ Duplicate content hash — skipping  channel='{}' hash={}",
                    dto.sourceName(), contentHash.substring(0, 12) + "...");
            metrics.recordDuplicate(dto.sourceType(), dto.sourceName(), "content_hash");
        }

        JobSource source = null;
        if (dto.sourceId() != null) {
            source = jobSourceRepository.findById(dto.sourceId()).orElse(null);
        }

        RawMessage rawMessage = RawMessage.builder()
                .source(source)
                .sourceType(dto.sourceType())
                .sourceName(dto.sourceName())
                .sourceChannelId(dto.sourceChannelId())
                .externalMessageId(dto.externalMessageId())
                .rawText(dto.text())
                .url(dto.url())
                .publishedAt(dto.publishedAt())
                .contentHash(contentHash)
                .processed(false)
                .build();

        RawMessage saved = rawMessageRepository.save(rawMessage);

        // Publish to queue for async processing
        RawJobMessage queueMessage = RawJobMessage.builder()
                .sourceType(dto.sourceType())
                .sourceName(dto.sourceName())
                .sourceChannelId(dto.sourceChannelId())
                .sourceId(source != null ? source.getId() : null)
                .externalMessageId(dto.externalMessageId())
                .rawText(dto.text())
                .url(dto.url())
                .publishedAt(dto.publishedAt())
                .build();

        rabbitTemplate.convertAndSend(RabbitQueues.EXCHANGE, RabbitQueues.RAW_ROUTING_KEY, queueMessage);
        metrics.recordIngested(dto.sourceType(), dto.sourceName());
        log.info("[INGEST] ✔ Accepted & queued  channel='{}' channelId={} msgId={} rawId={}",
                dto.sourceName(), dto.sourceChannelId(), dto.externalMessageId(), saved.getId());

        return saved;
    }

    @Transactional
    public void markProcessed(Long id) {
        rawMessageRepository.findById(id).ifPresent(m -> {
            m.setProcessed(true);
            rawMessageRepository.save(m);
        });
    }
}
