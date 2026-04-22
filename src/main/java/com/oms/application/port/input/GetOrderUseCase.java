package com.oms.application.port.input;

import com.oms.domain.model.Order;
import com.oms.domain.model.OrderStatus;
import com.oms.domain.model.PagedResult;

import java.util.UUID;

/**
 * Use Case (Input Port): Defines operations to retrieve order information.
 * This includes fetching an individual order by its unique UUID and fetching 
 * a paginated list of all orders, optionally filtered by status.
 */
public interface GetOrderUseCase {
    Order getOrderById(UUID id);
    PagedResult<Order> getAllOrders(int page, int size, OrderStatus status);
}
