package com.oms.domain.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Objeto de Dominio que modela el evento 'Orden Creada'.
 * <p>
 * En Java de 14+, usamos "records" porque son perfectos para datos inmutables y de valor, 
 * ideales para serializar de forma transparente sobre Kafka.
 */
public record OrderCreatedEvent(
    UUID orderId,
    String customerName,
    BigDecimal totalAmount,
    String status
) {
}
