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
 * Infrastructure Adapter for PostgreSQL persistence.
 * This `@Component` implements the 'OrderRepositoryPort' defined by the Domain layer, 
 * bridging the business requirements with technical relational storage.
 */
@Component
@RequiredArgsConstructor
public class PostgresOrderAdapter implements OrderRepositoryPort {

    private final JpaOrderRepository jpaRepository;
    private final OrderPersistenceMapper mapper;

    /**
     * Persists an Order into the relational database.
     *
     * @param order The rich domain model to save.
     * @return The persisted Order as a domain model.
     */
    @Override
    @Transactional
    public Order save(Order order) {
        // 1. Translate pure business model -> database entity layer (with automatic relationship mapping).
        OrderJpaEntity entity = mapper.toJpaEntity(order);
        
        // 2. Execute relational database transaction via Spring and Hibernate.
        OrderJpaEntity savedEntity = jpaRepository.save(entity);
        
        // 3. Translate the Data Access Object (DAO) back into an enriched Domain Entity.
        return mapper.toDomainModel(savedEntity);
    }

    /**
     * Retrieves an order by its unique identifier.
     *
     * @param id Unique identifier of the order.
     * @return An Optional containing the Order domain model if found.
     */
    @Override
    public Optional<Order> findById(UUID id) {
        // Direct fetcher utilizing the mapper on successful retrieval.
        return jpaRepository.findById(id).map(mapper::toDomainModel);
    }

    /**
     * Retrieves all orders with pagination and optional filtering by status.
     *
     * @param page   Zero-indexed page number.
     * @param size   Number of elements per page.
     * @param status Optional status filter.
     * @return A PagedResult containing the domain models and metadata.
     */
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
