package com.zak.jobhunter.enrichment;

import com.zak.jobhunter.enrichment.dto.UrlEnrichmentRequest;
import com.zak.jobhunter.messaging.RabbitQueues;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UrlEnrichmentListener {

    private static final Logger log = LoggerFactory.getLogger(UrlEnrichmentListener.class);

    private final UrlEnrichmentService enrichmentService;

    @RabbitListener(queues = RabbitQueues.URL_ENRICHMENT_QUEUE)
    public void onEnrichmentRequest(UrlEnrichmentRequest request) {
        log.info("[URL-LISTENER] ▶ Enriching  jobId={} url={}", request.jobId(), request.url());
        try {
            enrichmentService.fetchAndStore(request.jobId(), request.url());
        } catch (Exception ex) {
            log.error("[URL-LISTENER] Failed  jobId={} error={}", request.jobId(), ex.getMessage(), ex);
            throw ex; // re-throw → Spring AMQP retry / DLQ
        }
    }
}
