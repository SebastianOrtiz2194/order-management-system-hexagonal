package com.oms.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Centralized Kafka Configuration: includes topic definitions, error handling, 
 * and Dead Letter Queue (DLQ) mechanics.
 * <p>
 * Spring Boot auto-configures the {@code KafkaTemplate}, {@code ProducerFactory}, 
 * and {@code ConsumerFactory} using properties defined in {@code application.yml}. 
 * This class specifically defines beans that require explicit customization.
 */
@Configuration
@Slf4j
public class KafkaConfig {

    public static final String ORDER_EVENTS_TOPIC = "order-events";

    /**
     * Standard Spring Kafka suffix for Dead Letter Topics.
     * Messages that exhaust all retry attempts are automatically rerouted here.
     */
    public static final String ORDER_EVENTS_DLT = "order-events.DLT";

    /**
     * Maximum number of retry attempts before message redirection to DLT.
     */
    private static final long MAX_RETRY_ATTEMPTS = 3;

    /**
     * Fixed interval between retries in milliseconds.
     */
    private static final long RETRY_INTERVAL_MS = 1_000L;

    // ===================================================================
    // TOPICS
    // ===================================================================

    /**
     * Primary topic for order-related events.
     * 3 partitions allow for workload parallelization across multiple consumers 
     * in the same group.
     */
    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(ORDER_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)  // Configured for 1 Broker in local development environments.
                .build();
    }

    /**
     * Dead Letter Topic (DLT) for messages that failed after exhausting retries.
     * <p>
     * A single partition is sufficient given the expected low volume of failed messages.
     */
    @Bean
    public NewTopic orderEventsDltTopic() {
        return TopicBuilder.name(ORDER_EVENTS_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }

    // ===================================================================
    // ERROR HANDLING & DLQ
    // ===================================================================

    /**
     * Configures the {@link DeadLetterPublishingRecoverer} to route failing messages 
     * to the DLT after retry exhaustion. Original headers (including stack traces) 
     * are preserved for easier manual diagnosis.
     *
     * @param kafkaOperations The {@code KafkaTemplate} used for DLT publishing.
     * @return The configured recoverer.
     */
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaOperations<Object, Object> kafkaOperations) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaOperations,
                // Routing strategy: route all failures to the fixed DLT.
                (ConsumerRecord<?, ?> record, Exception ex) -> new org.apache.kafka.common.TopicPartition(
                        ORDER_EVENTS_DLT, -1  // -1 allows Kafka to determine the partition.
                ));

        log.info("DeadLetterPublishingRecoverer configured — failed messages route to [{}]", ORDER_EVENTS_DLT);
        return recoverer;
    }

    /**
     * Configures the global {@link DefaultErrorHandler} strategy for all 
     * {@code @KafkaListener} instances.
     * <p>
     * Behavior:
     * <ol>
     *   <li>Retries up to {@value MAX_RETRY_ATTEMPTS} times with a {@value RETRY_INTERVAL_MS}ms interval.</li>
     *   <li>On total exhaustion, reroutes the message to the DLT via the recoverer.</li>
     * </ol>
     *
     * @param recoverer The DLT redirection component.
     * @return The configured error handler.
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        FixedBackOff backOff = new FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRY_ATTEMPTS);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // Operational visibility for each retry attempt.
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("|| KAFKA RETRY || Attempt {}/{} for topic [{}], partition [{}], offset [{}]. Error: {}",
                        deliveryAttempt, MAX_RETRY_ATTEMPTS,
                        record.topic(), record.partition(), record.offset(),
                        ex.getMessage())
        );

        log.info("DefaultErrorHandler configured — maxRetries={}, intervalMs={}, DLT=[{}]",
                MAX_RETRY_ATTEMPTS, RETRY_INTERVAL_MS, ORDER_EVENTS_DLT);

        return errorHandler;
    }
}
