package com.oms.infrastructure.adapter.output.messaging;

import com.oms.application.port.output.OrderEventPublisherPort;
import com.oms.domain.event.OrderCreatedEvent;
import com.oms.infrastructure.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Adaptador de Salida apuntando a nuestro Kafka broker configurado.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaOrderEventPublisher implements OrderEventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(OrderCreatedEvent event) {
        // Enlazar la Key del evento Kafka explícitamente al orderId para forzar el
        // Kafka Routing Pattern (Garantizando la semántica de entrega ordenada dentro de esa partición)
        String kafkaKey = event.orderId().toString();

        log.info("Publishing OrderCreatedEvent to Kafka Topic: [{}] -> Key: [{}]", KafkaConfig.ORDER_EVENTS_TOPIC, kafkaKey);
        
        kafkaTemplate.send(KafkaConfig.ORDER_EVENTS_TOPIC, kafkaKey, event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Event successfully persisted on partition {}", result.getRecordMetadata().partition());
                } else {
                    log.error("Failed persisting Event on Kafka!", ex);
                    // Opcional: Para Resiliencia, aquí podría enrutarse a un topic DLQ (Dead Letter Queue) o tabla de Outbox en DB.
                }
            });
    }
}
