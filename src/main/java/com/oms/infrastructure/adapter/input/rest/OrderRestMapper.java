package com.oms.infrastructure.adapter.input.rest;

import com.oms.domain.model.Order;
import com.oms.domain.model.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * REST integration mapper specifically bound to the Input Adapter boundary.
 * The Controller natively orchestrates this Mapper to structurally translate 
 * DTOs into reliable Business Model classes and vice versa. This effectively ensures 
 * that the frontend schema cannot directly mandate backend core structures.
 */
@Mapper(componentModel = "spring")
public interface OrderRestMapper {

    // When interpreting a creation command, explicit instructions are made to ignore structural 
    // properties that the Domain is exclusively accountable for generating or verifying dynamically.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Order toDomainCommand(OrderDTOs.CreateOrderRequest request);

    OrderItem toDomainItem(OrderDTOs.OrderItemRequest request);

    // During data retrieval translation, formatting rules can reliably be attached here.
    // MapStruct handles LocalDateTime -> String bindings flawlessly by design.
    OrderDTOs.OrderResponse toResponseDto(Order order);

    OrderDTOs.OrderItemResponse toResponseItemDto(OrderItem item);
}
