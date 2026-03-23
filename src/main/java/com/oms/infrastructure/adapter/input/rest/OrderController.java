package com.oms.infrastructure.adapter.input.rest;

import com.oms.application.port.input.CreateOrderUseCase;
import com.oms.application.port.input.GetOrderUseCase;
import com.oms.application.port.input.UpdateOrderStatusUseCase;
import com.oms.domain.model.Order;
import com.oms.domain.model.OrderStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Adaptador de Entrada: El REST Controller actúa delegando toda inteligencia a los
 * Puertos de Entrada (`UseCases`). Jamás deberías ver un '@Transactional' u operaciones
 * de negocio de base de datos aquí.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final OrderRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDTOs.OrderResponse createOrder(@Valid @RequestBody OrderDTOs.CreateOrderRequest request) {
        Order command = mapper.toDomainCommand(request);
        Order savedOrderDomain = createOrderUseCase.createOrder(command);
        return mapper.toResponseDto(savedOrderDomain);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public OrderDTOs.OrderResponse getOrderById(@PathVariable UUID id) {
        Order order = getOrderUseCase.getOrderById(id);
        return mapper.toResponseDto(order);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<OrderDTOs.OrderResponse> getAllOrders() {
        return getOrderUseCase.getAllOrders().stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.OK)
    public OrderDTOs.OrderResponse updateStatus(
            @PathVariable UUID id, 
            @Valid @RequestBody OrderDTOs.UpdateStatusRequest request) {
        
        OrderStatus newStatusEnum = OrderStatus.valueOf(request.status().toUpperCase());
        Order updated = updateOrderStatusUseCase.updateStatus(id, newStatusEnum);
        return mapper.toResponseDto(updated);
    }
}
