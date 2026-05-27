package com.zak.jobhunter.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

import java.util.Map;

@Configuration
public class RabbitConfig {

    // ── Dead-letter exchange ──────────────────────────────────────────────

    @Bean
    public DirectExchange dlx() {
        return new DirectExchange(RabbitQueues.DLX, true, false);
    }

    // ── Main exchange ─────────────────────────────────────────────────────

    @Bean
    public DirectExchange jobsExchange() {
        return new DirectExchange(RabbitQueues.EXCHANGE, true, false);
    }

    // ── Raw messages ──────────────────────────────────────────────────────

    @Bean Queue rawJobMessagesDlq() {
        return QueueBuilder.durable(RabbitQueues.RAW_DLQ).build();
    }

    @Bean Queue rawJobMessagesQueue() {
        return QueueBuilder.durable(RabbitQueues.RAW_QUEUE)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", RabbitQueues.DLX,
                        "x-dead-letter-routing-key", RabbitQueues.RAW_DLQ))
                .build();
    }

    @Bean Binding rawQueueBinding(Queue rawJobMessagesQueue, DirectExchange jobsExchange) {
        return BindingBuilder.bind(rawJobMessagesQueue).to(jobsExchange).with(RabbitQueues.RAW_ROUTING_KEY);
    }

    @Bean Binding rawDlqBinding(Queue rawJobMessagesDlq, DirectExchange dlx) {
        return BindingBuilder.bind(rawJobMessagesDlq).to(dlx).with(RabbitQueues.RAW_DLQ);
    }

    // ── Matched jobs ──────────────────────────────────────────────────────

    @Bean Queue matchedJobsDlq() {
        return QueueBuilder.durable(RabbitQueues.MATCHED_DLQ).build();
    }

    @Bean Queue matchedJobsQueue() {
        return QueueBuilder.durable(RabbitQueues.MATCHED_QUEUE)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", RabbitQueues.DLX,
                        "x-dead-letter-routing-key", RabbitQueues.MATCHED_DLQ))
                .build();
    }

    @Bean Binding matchedQueueBinding(Queue matchedJobsQueue, DirectExchange jobsExchange) {
        return BindingBuilder.bind(matchedJobsQueue).to(jobsExchange).with(RabbitQueues.MATCHED_ROUTING_KEY);
    }

    @Bean Binding matchedDlqBinding(Queue matchedJobsDlq, DirectExchange dlx) {
        return BindingBuilder.bind(matchedJobsDlq).to(dlx).with(RabbitQueues.MATCHED_DLQ);
    }

    // ── AI analysis ───────────────────────────────────────────────────────

    @Bean Queue aiAnalysisDlq() {
        return QueueBuilder.durable(RabbitQueues.AI_ANALYSIS_DLQ).build();
    }

    @Bean Queue aiAnalysisQueue() {
        return QueueBuilder.durable(RabbitQueues.AI_ANALYSIS_QUEUE)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", RabbitQueues.DLX,
                        "x-dead-letter-routing-key", RabbitQueues.AI_ANALYSIS_DLQ))
                .build();
    }

    @Bean Binding aiAnalysisQueueBinding(Queue aiAnalysisQueue, DirectExchange jobsExchange) {
        return BindingBuilder.bind(aiAnalysisQueue).to(jobsExchange).with(RabbitQueues.AI_ANALYSIS_ROUTING_KEY);
    }

    @Bean Binding aiAnalysisDlqBinding(Queue aiAnalysisDlq, DirectExchange dlx) {
        return BindingBuilder.bind(aiAnalysisDlq).to(dlx).with(RabbitQueues.AI_ANALYSIS_DLQ);
    }

    @Bean Queue aiResultDlq() {
        return QueueBuilder.durable(RabbitQueues.AI_RESULT_DLQ).build();
    }

    @Bean Queue aiResultQueue() {
        return QueueBuilder.durable(RabbitQueues.AI_RESULT_QUEUE)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", RabbitQueues.DLX,
                        "x-dead-letter-routing-key", RabbitQueues.AI_RESULT_DLQ))
                .build();
    }

    @Bean Binding aiResultQueueBinding(Queue aiResultQueue, DirectExchange jobsExchange) {
        return BindingBuilder.bind(aiResultQueue).to(jobsExchange).with(RabbitQueues.AI_RESULT_ROUTING_KEY);
    }

    @Bean Binding aiResultDlqBinding(Queue aiResultDlq, DirectExchange dlx) {
        return BindingBuilder.bind(aiResultDlq).to(dlx).with(RabbitQueues.AI_RESULT_DLQ);
    }

    // ── URL enrichment ────────────────────────────────────────────────────

    @Bean Queue urlEnrichmentDlq() {
        return QueueBuilder.durable(RabbitQueues.URL_ENRICHMENT_DLQ).build();
    }

    @Bean Queue urlEnrichmentQueue() {
        return QueueBuilder.durable(RabbitQueues.URL_ENRICHMENT_QUEUE)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", RabbitQueues.DLX,
                        "x-dead-letter-routing-key", RabbitQueues.URL_ENRICHMENT_DLQ))
                .build();
    }

    @Bean Binding urlEnrichmentQueueBinding(Queue urlEnrichmentQueue, DirectExchange jobsExchange) {
        return BindingBuilder.bind(urlEnrichmentQueue).to(jobsExchange).with(RabbitQueues.URL_ENRICHMENT_ROUTING_KEY);
    }

    @Bean Binding urlEnrichmentDlqBinding(Queue urlEnrichmentDlq, DirectExchange dlx) {
        return BindingBuilder.bind(urlEnrichmentDlq).to(dlx).with(RabbitQueues.URL_ENRICHMENT_DLQ);
    }

    // ── Message converter ─────────────────────────────────────────────────

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    // ── Listener container with retry ─────────────────────────────────────

    @Bean
    public RetryOperationsInterceptor retryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(3_000, 2.0, 30_000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    /**
     * Default listener factory used for all @RabbitListener containers.
     * AI analysis queue uses a separate factory with longer back-off (see {@code aiRetryInterceptor}).
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter,
            RetryOperationsInterceptor retryInterceptor) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(retryInterceptor);
        return factory;
    }

    /**
     * Listener factory for the AI analysis queue.
     * Uses a much longer back-off so rate-limited requests are retried gradually.
     */
    @Bean
    public RetryOperationsInterceptor aiRetryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(60_000, 3.0, 600_000)   // 60s → 180s → 540s
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory aiListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter,
            RetryOperationsInterceptor aiRetryInterceptor) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(aiRetryInterceptor);
        return factory;
    }
}
