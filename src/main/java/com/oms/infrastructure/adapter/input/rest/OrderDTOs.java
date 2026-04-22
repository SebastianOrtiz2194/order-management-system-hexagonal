package com.oms.infrastructure.adapter.input.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

/**
 * Data Transfer Objects (DTOs) specifically tailored for REST requests and responses.
 * These structures isolate the domain from infrastructure presentation concerns.
 */
public class OrderDTOs {

    /**
     * Inbound Payload (Creation Request). Implements validations precisely at the 'boundary' 
     * to outright reject malformed anomalies prior to interacting with internal Domain validators.
     */
    public record CreateOrderRequest(
            @NotBlank(message = "Customer name is mandatory")
            String customerName,

            @NotEmpty(message = "The order cannot be empty")
            List<OrderItemRequest> items
    ) {}

    public record OrderItemRequest(
            @NotBlank(message = "Product ID cannot be blank") String productId,
            @NotBlank(message = "Product name cannot be blank") String productName,
            @Positive(message = "Quantity must be strictly positive") int quantity,
            @NotNull @Positive(message = "Unit price must be strictly positive") BigDecimal unitPrice
    ) {}

    public record UpdateStatusRequest(
            @NotBlank(message = "The new target status is mandatory")
            String status
    ) {}

    /**
     * Outbound Payload (Response). Ensures internal domain modeling or underlying 
     * technical database attributes are willfully abstracted from the API consumer.
     */
    public record OrderResponse(
            String id,
            String customerName,
            List<OrderItemResponse> items,
            String status,
            BigDecimal totalAmount,
            String createdAt,
            String updatedAt
    ) {}

    public record OrderItemResponse(
            String productId,
            String productName,
            int quantity,
            BigDecimal unitPrice
    ) {}

    /**
     * Structure tailored for paginated API responses.
     */
    public record PagedResponse<T>(
            List<T> content,
            int currentPage,
            int totalPages,
            long totalElements
    ) {}
}
