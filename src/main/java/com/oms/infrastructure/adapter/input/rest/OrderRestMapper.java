package com.oms.infrastructure.adapter.input.rest;

import com.oms.domain.model.Order;
import com.oms.domain.model.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapeador que vive solo en la capa Input Adapter.
 * El Controlador REST usa este Mapper para transformar DTOs <-> Clases de Negocio,
 * asegurando que el Frontend no dicta la forma de las Entradas del Backend.
 */
@Mapper(componentModel = "spring")
public interface OrderRestMapper {

    // Al crear, ignoramos campos que el Dominio generará/calculará (Fase de Validación)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Order toDomainCommand(OrderDTOs.CreateOrderRequest request);

    OrderItem toDomainItem(OrderDTOs.OrderItemRequest request);

    OrderDTOs.OrderResponse toResponseDto(Order order);
    OrderDTOs.OrderItemResponse toResponseItemDto(OrderItem item);
}
