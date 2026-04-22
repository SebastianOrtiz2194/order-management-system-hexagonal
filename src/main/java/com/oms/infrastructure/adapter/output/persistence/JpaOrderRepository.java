package com.oms.infrastructure.adapter.output.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Standard Spring Data JPA Repository interface.
 * This is an infrastructure tool used by the Postgres adapter, not a domain port.
 * It provides out-of-the-box CRUD operations for OrderJpaEntity.
 */
@Repository
public interface JpaOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {
    
    /**
     * Finds orders filtered by status with pagination support.
     *
     * @param status   The order status as a string.
     * @param pageable Pagination and sorting information.
     * @return A Page of OrderJpaEntity.
     */
    Page<OrderJpaEntity> findByStatus(String status, Pageable pageable);
}
