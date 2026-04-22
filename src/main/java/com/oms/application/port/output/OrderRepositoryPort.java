package com.oms.application.port.output;

import com.oms.domain.model.Order;
import com.oms.domain.model.OrderStatus;
import com.oms.domain.model.PagedResult;

import java.util.Optional;
import java.util.UUID;

/**
 * Output Port: Persistence Adapter Requirement.
 * Denotes that the application needs a mechanism (a lower layer) capable of 
 * storing and retrieving Order records, without imposing implementation details 
 * like JPA, SQL, or NoSQL databases.
 */
public interface OrderRepositoryPort {
    Order save(Order order);
    Optional<Order> findById(UUID id);
    PagedResult<Order> findAll(int page, int size, OrderStatus status);
}
