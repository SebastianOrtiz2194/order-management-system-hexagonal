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
    public void consumeOrderCreated(
            @Payload OrderCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("|| KAFKA CONSUMER || -> Event received from topic [{}], partition [{}], offset [{}]",
                topic, partition, offset);

        // ------------------------------------------------------------------
        // 1. Validación defensiva del payload
        // ------------------------------------------------------------------
        if (event == null) {
            log.error("|| KAFKA CONSUMER || -> Received NULL event payload — skipping. " +
                    "Topic [{}], partition [{}], offset [{}]", topic, partition, offset);
            // No lanzamos excepción: un null payload no se corregirá con reintentos.
            // El DefaultErrorHandler lo enviaría al DLT de todas formas, pero evitamos
            // reintentos innecesarios retornando directamente.
            return;
        }

        if (event.orderId() == null || event.customerName() == null || event.status() == null) {
            log.error("|| KAFKA CONSUMER || -> Event has null required fields " +
                            "(orderId={}, customerName={}, status={}). Skipping message at offset [{}].",
                    event.orderId(), event.customerName(), event.status(), offset);
            return;
        }

        // ------------------------------------------------------------------
        // 2. Procesamiento del evento con manejo de errores de negocio
        // ------------------------------------------------------------------
        try {
            log.info("|| KAFKA CONSUMER || -> Processing Order ID: {}", event.orderId());

            // Parseo seguro del status con validación explícita
            OrderStatus status = parseOrderStatus(event.status());

            // Reconstrucción del modelo de dominio a partir del evento
            // Nota: este es un modelo parcial (sin items) usado exclusivamente para cache warm-up.
            // En un escenario real, se invocaría un UseCase dedicado.
            Order reconstitutedOrder = Order.builder()
                    .id(event.orderId())
                    .customerName(event.customerName())
                    .status(status)
                    .totalAmount(event.totalAmount())
                    .build();

            cachePort.save(reconstitutedOrder);

            log.info("|| KAFKA CONSUMER || -> Cache pre-warmed successfully for Order ID: {} (status: {})",
                    event.orderId(), status);

        } catch (IllegalArgumentException ex) {
            // Status inválido u otro error de parsing — no tiene sentido reintentar
            log.error("|| KAFKA CONSUMER || -> Non-retryable error processing event for Order ID [{}]: {}. " +
                            "Message will NOT be retried.",
                    event.orderId(), ex.getMessage());
            // No relanzamos: evita que el DefaultErrorHandler reintente un error irrecuperable

        } catch (Exception ex) {
            // Error inesperado (e.g. Redis timeout, serialización) — permitimos reintento
            log.error("|| KAFKA CONSUMER || -> Unexpected error processing event for Order ID [{}]. " +
                            "Message will be retried by ErrorHandler. Error: {}",
                    event.orderId(), ex.getMessage(), ex);
            // Relanzamos para que el DefaultErrorHandler aplique la política de reintentos + DLQ
            throw ex;
        }
    }

    /**
     * Parsea de forma segura un String a {@link OrderStatus}.
     * Lanza {@link IllegalArgumentException} si el valor no corresponde a un estado válido.
     *
     * @param statusValue el valor string del status proveniente del evento
     * @return el {@link OrderStatus} correspondiente
     * @throws IllegalArgumentException si el valor no es un status válido
     */
    private OrderStatus parseOrderStatus(String statusValue) {
        try {
            return OrderStatus.valueOf(statusValue);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    String.format("Invalid OrderStatus value '%s' in Kafka event. Valid values: %s",
                            statusValue, java.util.Arrays.toString(OrderStatus.values())),
                    ex);
        }
    }
}
