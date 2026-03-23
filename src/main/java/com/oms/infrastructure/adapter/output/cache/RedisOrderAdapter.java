package com.oms.infrastructure.adapter.output.cache;

import com.oms.application.port.output.OrderCachePort;
import com.oms.domain.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Adaptador de Infraestructura para el Caché.
 */
@Component
@RequiredArgsConstructor
@Slf4j // Herramienta de observabilidad
public class RedisOrderAdapter implements OrderCachePort {

    private final RedisTemplate<String, Order> redisTemplate;
    
    // Un prefijo estándar ayuda a la administración masiva en sistemas corporativos -> "order:c2ba..."
    private static final String KEY_PREFIX = "order:";
    private static final long TTL_HOURS = 24;

    @Override
    public void save(Order order) {
        String key = KEY_PREFIX + order.getId().toString();
        
        // Push atómicamente JSON contra Node Redis. Expirará tras 24hr de inactividad
        redisTemplate.opsForValue().set(key, order, TTL_HOURS, TimeUnit.HOURS);
        log.info("Cached Object into Redis successfully! - TTL: 24Hr - Key: {}", key);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        String key = KEY_PREFIX + id.toString();
        // Null safe access a redis
        Order order = redisTemplate.opsForValue().get(key);
        
        if (order != null) log.debug("Cache hit for {}", key);
        else log.debug("Cache miss for {}", key);
        
        return Optional.ofNullable(order);
    }

    @Override
    public void evict(UUID id) {
        String key = KEY_PREFIX + id.toString();
        redisTemplate.delete(key);
        log.info("Evicted Cache Key due to State Mutation: {}", key);
    }

    @Override
    public boolean existsById(UUID id) {
        String key = KEY_PREFIX + id.toString();
        Boolean hasKey = redisTemplate.hasKey(key);
        return hasKey != null && hasKey;
    }
}
