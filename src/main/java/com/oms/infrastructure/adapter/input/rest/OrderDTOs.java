package com.oms.infrastructure.adapter.input.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

public class OrderDTOs {

    /**
     * Payload de Recepción (Creación). Validaciones en la 'frontera' evitan peticiones
     * mal formadas incluso antes de cruzar hacia los validadores del Dominio interno.
     */
    public record CreateOrderRequest(
            @NotBlank(message = "El cliente es obligatorio")
            String customerName,

            @NotEmpty(message = "La orden no puede estar vacía")
            List<OrderItemRequest> items
    ) {}

    public record OrderItemRequest(
            @NotBlank String productId,
            @NotBlank String productName,
            @Positive int quantity,
            @NotNull @Positive BigDecimal unitPrice
    ) {}

    public record UpdateStatusRequest(
            @NotBlank(message = "El nuevo estado es obligatorio")
            String status
    ) {}

    /**
     * Payload de Retorno, omite información interna o técnica de bases de datos
     */
    public record OrderResponse(
            String id,
            String customerName,
            List<OrderItemResponse> items,
            String status,
            BigDecimal totalAmount,
            String createdAt
    ) {}

    public record OrderItemResponse(
            String productId,
            String productName,
            int quantity,
            BigDecimal unitPrice
    ) {}
}
