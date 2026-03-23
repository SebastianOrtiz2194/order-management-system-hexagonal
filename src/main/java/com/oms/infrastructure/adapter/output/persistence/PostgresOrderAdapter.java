package com.oms.infrastructure.adapter.output.persistence;

import com.oms.application.port.output.OrderRepositoryPort;
import com.oms.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * El Adaptador de Infraestructura para PostgreSQL.
 * Este componente `@Component` concreta la interfaz 'OrderRepositoryPort' dictada
 * por el Dominio hacia el exterior. 
 */
@Component
@RequiredArgsConstructor
public class PostgresOrderAdapter implements OrderRepositoryPort {

    private final JpaOrderRepository jpaRepository;
    private final OrderPersistenceMapper mapper;

    @Override
    public Order save(Order order) {
        // 1. Traducir puro negocio -> capa base de datos.
        OrderJpaEntity entity = mapper.toJpaEntity(order);
        
        // Link parent reference for JPA bi-directional relationship
        if (entity.getItems() != null) {
            entity.getItems().forEach(item -> item.setOrder(entity));
        }
        
        // 2. Transacción de la Base Relacional a través de Spring y Hibernate.
        OrderJpaEntity savedEntity = jpaRepository.save(entity);
        
        // 3. Traducir nuevamente el Data Access Oject (DAO) -> Entidad de Dominio enriquecida (vuelve al Hexágono).
        return mapper.toDomainModel(savedEntity);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        // Fetcher directo que se aprovecha del mapper si encuentra contenido
        return jpaRepository.findById(id).map(mapper::toDomainModel);
    }

    @Override
    public List<Order> findAll() {
        return mapper.toDomainList(jpaRepository.findAll());
    }
}
