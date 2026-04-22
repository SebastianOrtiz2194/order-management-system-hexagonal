package com.oms.infrastructure.adapter.input.rest;

import com.oms.application.port.input.CreateOrderUseCase;
import com.oms.application.port.input.GetOrderUseCase;
import com.oms.application.port.input.UpdateOrderStatusUseCase;
import com.oms.domain.model.Order;
import com.oms.domain.model.OrderStatus;
import com.oms.domain.model.PagedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Input Adapter: The REST Controller. Operates exclusively by delegating all business mechanics 
 * and intelligence downstream to the designated Input Ports (`UseCases`).
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "Endpoints for creating, retrieving, and updating orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final OrderRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new order", description = "Validates and creates a new order in the system, returning 201 Created and the new order.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    public OrderDTOs.OrderResponse createOrder(@Valid @RequestBody OrderDTOs.CreateOrderRequest request) {
        Order command = mapper.toDomainCommand(request);
        Order savedOrderDomain = createOrderUseCase.createOrder(command);
        return mapper.toResponseDto(savedOrderDomain);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get order by ID", description = "Retrieves an order by its UUID, fetching from Redis cache if available or PostgreSQL database.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found and returned"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public OrderDTOs.OrderResponse getOrderById(@PathVariable UUID id) {
        Order order = getOrderUseCase.getOrderById(id);
        return mapper.toResponseDto(order);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get all orders", description = "Retrieves all current orders in the system with pagination and optional filtering.")
    @ApiResponse(responseCode = "200", description = "Orders returned successfully")
    public OrderDTOs.PagedResponse<OrderDTOs.OrderResponse> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        
        OrderStatus statusFilter = status != null ? OrderStatus.valueOf(status.toUpperCase()) : null;
        PagedResult<Order> pagedResult = getOrderUseCase.getAllOrders(page, size, statusFilter);
        
        List<OrderDTOs.OrderResponse> content = pagedResult.content().stream()
                .map(mapper::toResponseDto)
                .toList();

        return new OrderDTOs.PagedResponse<>(
                content,
                pagedResult.page(),
                pagedResult.totalPages(),
                pagedResult.totalElements()
        );
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Update order status", description = "Updates an order's status and returns the updated order.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Status transition invalid"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public OrderDTOs.OrderResponse updateStatus(
            @PathVariable UUID id, 
            @Valid @RequestBody OrderDTOs.UpdateStatusRequest request) {
        
        OrderStatus newStatusEnum = OrderStatus.valueOf(request.status().toUpperCase());
        Order updated = updateOrderStatusUseCase.updateStatus(id, newStatusEnum);
        return mapper.toResponseDto(updated);
    }
}
