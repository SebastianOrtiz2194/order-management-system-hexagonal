package com.oms.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Domain event representing the successful creation of an order.
 * This event is published to notify other bounded contexts or external systems
 * that an order has been initialized and validated.
 */
public record OrderCreatedEvent(
    UUID orderId,
    String customerName,
    BigDecimal totalAmount,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<OrderItemEvent> items
) {
    /**
     * DTO for the items within the order creation event.
     */
    public record OrderItemEvent(
        String productId,
        String productName,
        int quantity,
        BigDecimal unitPrice
    ) {}
}
