package com.oms.infrastructure.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Interface mágica de Spring Data JPA.
 * No es un purto de dominio, es una herramienta de infraestructura que usará el Adaptador Postgres.
 */
@Repository
public interface JpaOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {
}
