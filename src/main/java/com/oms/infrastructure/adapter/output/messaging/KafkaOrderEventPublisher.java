package com.oms.infrastructure.adapter.output.messaging;

import com.oms.application.port.output.OrderEventPublisherPort;
import com.oms.domain.event.OrderCreatedEvent;
import com.oms.infrastructure.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Output Adapter targeting the configured Kafka broker.
 * Responsible for transmitting domain events to external systems.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaOrderEventPublisher implements OrderEventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes an OrderCreatedEvent to Kafka.
     * Uses the orderId as the message key to implement the Kafka Routing Pattern, 
     * ensuring strictly ordered delivery within the specific partition.
     *
     * @param event The domain event to publish.
     */
    @Override
    public void publish(OrderCreatedEvent event) {
        // Explicitly bind the Kafka event key to the orderId to enforce 
        // partition-level ordering semantics.
        String kafkaKey = event.orderId().toString();

        log.info("Publishing OrderCreatedEvent to Kafka Topic: [{}] -> Key: [{}]", KafkaConfig.ORDER_EVENTS_TOPIC, kafkaKey);
        
        kafkaTemplate.send(KafkaConfig.ORDER_EVENTS_TOPIC, kafkaKey, event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Event successfully persisted on partition {}", result.getRecordMetadata().partition());
                } else {
                    log.error("Failed persisting Event on Kafka!", ex);
                    // Optional: For enhanced resilience, this could route to a Dead Letter Queue (DLQ) 
                    // or an Outbox table in the database.
                }
            });
    }
}
