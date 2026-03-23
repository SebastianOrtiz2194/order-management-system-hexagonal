package com.oms.application.port.input;

import com.oms.domain.model.Order;
import java.util.List;
import java.util.UUID;

/**
 * Caso de Uso (Input Port): "Obtener información del pedido basándose en su ID UUID."
 */
public interface GetOrderUseCase {
    Order getOrderById(UUID id);
    List<Order> getAllOrders();
}
