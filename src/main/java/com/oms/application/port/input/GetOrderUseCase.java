package com.oms.application.port.input;

import com.oms.domain.model.Order;
import com.oms.domain.model.OrderStatus;
import com.oms.domain.model.PagedResult;

import java.util.UUID;

/**
 * Caso de Uso (Input Port): "Obtener información del pedido basándose en su ID UUID."
 */
public interface GetOrderUseCase {
    Order getOrderById(UUID id);
    PagedResult<Order> getAllOrders(int page, int size, OrderStatus status);
}
