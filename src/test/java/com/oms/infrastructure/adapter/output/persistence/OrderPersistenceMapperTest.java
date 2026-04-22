package com.oms.infrastructure.adapter.output.persistence;

import com.oms.domain.model.Order;
import com.oms.domain.model.OrderItem;
import com.oms.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the OrderPersistenceMapper.
 * Verifies bidirectional mapping between rich Domain Models and anemic JPA Entities.
 */
class OrderPersistenceMapperTest {

    private final OrderPersistenceMapper mapper = Mappers.getMapper(OrderPersistenceMapper.class);

    @Test
    void shouldMapDomainToJpaEntity() {
        // Arrange
        UUID id = UUID.randomUUID();
        OrderItem item = OrderItem.builder().productId("p1").quantity(1).unitPrice(BigDecimal.TEN).build();
        Order order = Order.builder()
                .id(id)
                .customerName("Alice")
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.TEN)
                .items(List.of(item))
                .build();

        // Act
        OrderJpaEntity entity = mapper.toJpaEntity(order);

        // Assert
        assertEquals(id, entity.getId());
        assertEquals("Alice", entity.getCustomerName());
        assertEquals("PENDING", entity.getStatus());
        assertEquals(1, entity.getItems().size());
        assertEquals(entity, entity.getItems().get(0).getOrder()); // Bi-directional entity link verification
    }

    @Test
    void shouldMapJpaEntityToDomain() {
        // Arrange
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        OrderItemJpaEntity itemEntity = OrderItemJpaEntity.builder()
                .productId("p2")
                .productName("Prod 2")
                .quantity(3)
                .unitPrice(new BigDecimal("5.00"))
                .build();
        
        OrderJpaEntity entity = OrderJpaEntity.builder()
                .id(id)
                .customerName("Bob")
                .status("CONFIRMED")
                .totalAmount(new BigDecimal("15.00"))
                .createdAt(now)
                .items(List.of(itemEntity))
                .build();

        // Act
        Order domain = mapper.toDomainModel(entity);

        // Assert
        assertEquals(id, domain.getId());
        assertEquals("Bob", domain.getCustomerName());
        assertEquals(OrderStatus.CONFIRMED, domain.getStatus());
        assertEquals(new BigDecimal("15.00"), domain.getTotalAmount());
        assertEquals(now, domain.getCreatedAt());
        assertEquals(1, domain.getItems().size());
        assertEquals("p2", domain.getItems().get(0).getProductId());
    }

    @Test
    void shouldHandleNullItems() {
        // Arrange
        Order order = Order.builder()
                .customerName("No Items")
                .items(null)
                .build();

        // Act
        OrderJpaEntity entity = mapper.toJpaEntity(order);

        // Assert
        assertNotNull(entity);
        assertNull(entity.getItems());
    }

    @Test
    void shouldMapListToDomain() {
        // Arrange
        OrderJpaEntity entity = OrderJpaEntity.builder()
                .customerName("List Test")
                .status("PENDING")
                .totalAmount(BigDecimal.ZERO)
                .items(Collections.emptyList())
                .build();

        // Act
        List<Order> domains = mapper.toDomainList(List.of(entity));

        // Assert
        assertEquals(1, domains.size());
        assertEquals("List Test", domains.get(0).getCustomerName());
    }
}
