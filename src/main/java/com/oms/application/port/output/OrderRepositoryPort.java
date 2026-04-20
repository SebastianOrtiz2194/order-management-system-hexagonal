package com.oms.application.port.output;

import com.oms.domain.model.Order;
import com.oms.domain.model.OrderStatus;
import com.oms.domain.model.PagedResult;

import java.util.Optional;
import java.util.UUID;

/**
 * Output Port (Adaptador de infraestructura necesario).
 * Indica que necesitamos que Alguien (una capa inferior) sea capaz de almacenar Order,
 * sin decirle a ese "Alguien" que debe usar JPA ni Base Relacionales.
 */
public interface OrderRepositoryPort {
    Order save(Order order);
    Optional<Order> findById(UUID id);
    PagedResult<Order> findAll(int page, int size, OrderStatus status);
}
