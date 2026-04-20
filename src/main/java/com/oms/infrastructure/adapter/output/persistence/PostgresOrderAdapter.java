package com.oms.infrastructure.adapter.output.persistence;

import com.oms.application.port.output.OrderRepositoryPort;
import com.oms.domain.model.Order;
import com.oms.domain.model.OrderStatus;
import com.oms.domain.model.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public Order save(Order order) {
        // 1. Traducir puro negocio -> capa base de datos (con links automáticos).
        OrderJpaEntity entity = mapper.toJpaEntity(order);
        
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
    public PagedResult<Order> findAll(int page, int size, OrderStatus status) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderJpaEntity> entityPage;
        
        if (status != null) {
            entityPage = jpaRepository.findByStatus(status.name(), pageable);
        } else {
            entityPage = jpaRepository.findAll(pageable);
        }

        List<Order> content = mapper.toDomainList(entityPage.getContent());
        
        return new PagedResult<>(
                content,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );
    }
}
