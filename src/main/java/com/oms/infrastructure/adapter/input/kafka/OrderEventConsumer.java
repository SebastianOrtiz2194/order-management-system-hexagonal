package com.oms.infrastructure.adapter.input.kafka;

import com.oms.application.port.output.OrderCachePort;
import com.oms.domain.event.OrderCreatedEvent;
import com.oms.domain.model.Order;
import com.oms.domain.model.OrderStatus;
import com.oms.infrastructure.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Input Adapter (Consumidor).
 * Podría considerarse "independiente", simulará un sistema Downstream que pre-calienta (Warm-Up) el Redis
 * una vez generada una orden.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderCachePort cachePort;

    @KafkaListener(topics = KafkaConfig.ORDER_EVENTS_TOPIC, groupId = "oms-consumer-group")
    public void consumeOrderCreated(@Payload OrderCreatedEvent event) {
        log.info("|| KAFKA CONSUMER || -> Event received processing Order ID: {}", event.orderId());
        
        // Aquí demostramos un patrón Event-Driven (Warm up) reconstruyendo el modelo 
        // a partir del evento, y persistiendo/precalentando la memoria (Cache).
        // En un escenario real, llamaríamos a otro UseCase.
        Order reconstitutedOrder = Order.builder()
                .id(event.orderId())
                .customerName(event.customerName())
                .status(OrderStatus.valueOf(event.status()))
                .totalAmount(event.totalAmount())
                .createdAt(event.createdAt())
                .updatedAt(event.updatedAt())
                .items(event.items().stream()
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
    }
}
