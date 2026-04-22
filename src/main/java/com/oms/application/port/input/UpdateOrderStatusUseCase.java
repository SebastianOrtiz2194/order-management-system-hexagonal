package com.oms.application.port.input;

import com.oms.domain.model.OrderStatus;
import java.util.UUID;
import com.oms.domain.model.Order;

/**
 * Use Case (Input Port): "Update the status of an existing order."
 * Exposes the operation to transition an order's state, delegating 
 * internal validations to the pure domain model.
 */
public interface UpdateOrderStatusUseCase {
    Order updateStatus(UUID id, OrderStatus newStatus);
}
