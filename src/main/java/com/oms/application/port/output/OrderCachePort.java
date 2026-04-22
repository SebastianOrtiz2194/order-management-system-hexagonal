package com.oms.application.port.output;

import com.oms.domain.model.Order;
import java.util.Optional;
import java.util.UUID;

/**
 * Output Port: Caching Requirement.
 * "We need to cache retrieved orders to improve performance."
 * The application layer doesn't know or care if the underlying implementation 
 * uses Redis, Memcached, or a simple HashMap. It simply depends on this contract.
 */
public interface OrderCachePort {
    void save(Order order);
    Optional<Order> findById(UUID id);
    void evict(UUID id);
    boolean existsById(UUID id);
}
