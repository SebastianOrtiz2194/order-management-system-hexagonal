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
 * Actúa como un servicio downstream que pre-calienta (Warm-Up) el caché Redis
 * al recibir eventos de creación de órdenes desde Kafka.
 * <p>
 * <b>Estrategia de resiliencia:</b>
 * <ul>
 *   <li>Validación explícita del payload antes de procesarlo (null-safety).</li>
 *   <li>Try-catch interno para errores de lógica de negocio (e.g. status inválido).</li>
 *   <li>Los errores de deserialización y las excepciones no capturadas son manejados
 *       por el {@code DefaultErrorHandler} configurado en {@link KafkaConfig},
 *       que reintentará hasta 3 veces y luego enviará el mensaje al Dead Letter Topic (DLT).</li>
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderCachePort cachePort;

    /**
     * Consume eventos {@link OrderCreatedEvent} desde el topic {@code order-events}.
     * Reconstruye un modelo de dominio parcial a partir del evento y lo persiste en
     * el caché Redis para acelerar futuras lecturas (patrón Cache Warm-Up).
     *
     * @param event     el evento de creación de orden deserializado por Spring Kafka
     * @param topic     nombre del topic de origen (inyectado por Spring para logging)
     * @param partition partición de Kafka desde la que se leyó el mensaje
     * @param offset    offset del mensaje dentro de la partición
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
            // Aquí demostramos un patrón Event-Driven (Warm up) reconstruyendo el modelo 
            // a partir del evento, y persistiendo/precalentando la memoria (Cache).
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
