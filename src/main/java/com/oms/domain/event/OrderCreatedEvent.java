package com.oms.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
    UUID orderId,
    String customerName,
    BigDecimal totalAmount,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<OrderItemEvent> items
) {
    public record OrderItemEvent(
        String productId,
        String productName,
        int quantity,
        BigDecimal unitPrice
    ) {}
}
