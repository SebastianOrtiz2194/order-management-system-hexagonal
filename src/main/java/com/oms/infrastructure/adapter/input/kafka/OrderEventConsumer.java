package com.oms.infrastructure.adapter.input.kafka;

import com.oms.application.port.output.OrderCachePort;
import com.oms.domain.event.OrderCreatedEvent;
import com.oms.domain.model.Order;
import com.oms.domain.model.OrderStatus;
import com.oms.infrastructure.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Input Adapter (Driving Adapter) — Kafka Consumer.
 * <p>
 * Acts as a downstream service that pre-warms the Redis cache by actively 
 * listening to order creation events from Kafka.
 * <p>
 * <b>Resilience Strategy:</b>
 * <ul>
 *   <li>Explicit validation of the payload before further processing (null-safety).</li>
 *   <li>Internal try-catch strategy to gracefully handle business logic errors (e.g., invalid status).</li>
 *   <li>Deserialization discrepancies and uncaught exceptions are intercepted and managed 
 *       by the {@code DefaultErrorHandler} configured in {@link KafkaConfig}, 
 *       which performs up to 3 retry attempts before routing the faulty message to a Dead Letter Topic (DLT).</li>
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderCachePort cachePort;

    /**
     * Consumes {@link OrderCreatedEvent} events sourced from the {@code order-events} topic.
     * Reconstitutes a partial domain model originating from the incoming event and subsequently 
     * persists it within the Redis cache to vastly accelerate subsequent read operations (Cache Warm-Up pattern).
     *
     * @param event     The deserialized order creation event provided by Spring Kafka.
     * @param topic     Originating topic name (Optional, typically injected by Spring for logging context).
     * @param partition The specific Kafka partition from which the message was consumed.
     * @param offset    The partition-specific message offset.
     */
    @KafkaListener(topics = KafkaConfig.ORDER_EVENTS_TOPIC, groupId = "oms-consumer-group")
    public void consumeOrderCreated(@Payload OrderCreatedEvent event) {
        if (event == null) {
            log.warn("|| KAFKA CONSUMER || -> Received null event payload. Skipping.");
            return;
        }

        if (event.orderId() == null || event.customerName() == null || event.status() == null) {
            log.warn("|| KAFKA CONSUMER || -> Received event with missing required fields. Skipping.");
            return;
        }

        log.info("|| KAFKA CONSUMER || -> Event received processing Order ID: {}", event.orderId());
        
        try {
            // Employs an Event-Driven (Warm-up) pattern here by rebuilding the domain model 
            // directly from the incoming event data and caching it securely.
            Order reconstitutedOrder = Order.builder()
                    .id(event.orderId())
                    .customerName(event.customerName())
                    .status(OrderStatus.valueOf(event.status()))
                    .totalAmount(event.totalAmount())
                    .createdAt(event.createdAt())
                    .updatedAt(event.updatedAt())
                    .items(event.items() == null ? java.util.Collections.emptyList() : event.items().stream()
                            .map(item -> com.oms.domain.model.OrderItem.builder()
                                    .productId(item.productId())
                                    .productName(item.productName())
                                    .quantity(item.quantity())
                                    .unitPrice(item.unitPrice())
                                    .build())
                            .toList())
                    .build();
            
            cachePort.save(reconstitutedOrder);
            log.info("|| KAFKA CONSUMER || -> Target Cache was Pre-Warmed correctly.");
        } catch (IllegalArgumentException e) {
            log.error("|| KAFKA CONSUMER || -> Invalid status received in event: {}. Skipping.", event.status());
        }
    }
}
