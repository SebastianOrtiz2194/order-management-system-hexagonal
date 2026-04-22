package com.oms.infrastructure.adapter.output.cache;

import com.oms.application.port.output.OrderCachePort;
import com.oms.domain.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Infrastructure Adapter for caching operations.
 * Implements the OrderCachePort using Redis as the underlying storage mechanism.
 */
@Component
@RequiredArgsConstructor
@Slf4j // Observability tool for logging
public class RedisOrderAdapter implements OrderCachePort {

    private final RedisTemplate<String, Order> redisTemplate;
    
    // Standard prefix for bulk administration in enterprise systems -> "order:c2ba..."
    private static final String KEY_PREFIX = "order:";
    private static final long TTL_HOURS = 24;

    /**
     * Persists an order into the Redis cache with a defined Time-To-Live (TTL).
     *
     * @param order The domain order object to cache.
     */
    @Override
    public void save(Order order) {
        try {
            String key = KEY_PREFIX + order.getId().toString();
            // Atomically push JSON to Redis node. Entry expires after 24 hours of inactivity.
            redisTemplate.opsForValue().set(key, order, TTL_HOURS, TimeUnit.HOURS);
            log.info("Cached Object into Redis successfully! - TTL: 24Hr - Key: {}", key);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis is down. Skipping cache write for order {}: {}", order.getId(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error writing to Redis cache: {}", e.getMessage());
        }
    }

    /**
     * Retrieves an order from the cache if it exists.
     *
     * @param id Unique identifier of the order.
     * @return An Optional containing the cached Order, or empty if not found or Redis is unavailable.
     */
    @Override
    public Optional<Order> findById(UUID id) {
        try {
            String key = KEY_PREFIX + id.toString();
            // Null-safe access to Redis
            Order order = redisTemplate.opsForValue().get(key);
            
            if (order != null) log.debug("Cache hit for {}", key);
            else log.debug("Cache miss for {}", key);
            
            return Optional.ofNullable(order);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis is down. Falling back to DB for order {}: {}", id, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error reading from Redis cache: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Removes an order from the cache. Typically invoked after a state mutation 
     * to prevent stale data.
     *
     * @param id Unique identifier of the order to evict.
     */
    @Override
    public void evict(UUID id) {
        try {
            String key = KEY_PREFIX + id.toString();
            redisTemplate.delete(key);
            log.info("Evicted Cache Key due to State Mutation: {}", key);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis is down. Skipping cache eviction for order {}: {}", id, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error evicting from Redis cache: {}", e.getMessage());
        }
    }

    /**
     * Checks if a specific order key exists in the cache.
     *
     * @param id Unique identifier of the order.
     * @return true if the key exists, false otherwise.
     */
    @Override
    public boolean existsById(UUID id) {
        try {
            String key = KEY_PREFIX + id.toString();
            Boolean hasKey = redisTemplate.hasKey(key);
            return hasKey != null && hasKey;
        } catch (Exception e) {
            log.warn("Redis error on existsById check. Assuming false: {}", e.getMessage());
            return false;
        }
    }
}
