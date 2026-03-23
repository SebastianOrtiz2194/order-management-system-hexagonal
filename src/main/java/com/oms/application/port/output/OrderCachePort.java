package com.oms.application.port.output;

import com.oms.domain.model.Order;
import java.util.Optional;
import java.util.UUID;

/**
 * Output Port. Requerimiento: "Me gustaría almacenar en caché las órdenes que busque".
 * ¿Redis, Memcached, HashMap? No tenemos idea ni nos importa a este nivel del código.
 */
public interface OrderCachePort {
    void save(Order order);
    Optional<Order> findById(UUID id);
    void evict(UUID id);
    boolean existsById(UUID id);
}
