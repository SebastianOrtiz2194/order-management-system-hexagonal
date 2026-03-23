package com.oms.application.port.input;

import com.oms.domain.model.OrderStatus;
import java.util.UUID;
import com.oms.domain.model.Order;

/**
 * Input Port: Actualizar el estado del pedido, asegurando las validaciones
 * necesarias en la capa de modelo puro.
 */
public interface UpdateOrderStatusUseCase {
    Order updateStatus(UUID id, OrderStatus newStatus);
}
