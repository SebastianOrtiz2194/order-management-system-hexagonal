package com.oms.infrastructure.adapter.output.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Interface mágica de Spring Data JPA.
 * No es un purto de dominio, es una herramienta de infraestructura que usará el Adaptador Postgres.
 */
@Repository
public interface JpaOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {
    Page<OrderJpaEntity> findByStatus(String status, Pageable pageable);
}
