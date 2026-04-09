package com.oms.infrastructure.adapter.input.rest;

import com.oms.domain.model.Order;
import com.oms.domain.model.OrderItem;
import com.oms.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderRestMapperTest {

    private final OrderRestMapper mapper = Mappers.getMapper(OrderRestMapper.class);

    @Test
    void shouldMapCreateRequestToDomain() {
        // Arrange
        OrderDTOs.OrderItemRequest itemRequest = new OrderDTOs.OrderItemRequest("prod-1", "Product 1", 2, new BigDecimal("10.50"));
        OrderDTOs.CreateOrderRequest request = new OrderDTOs.CreateOrderRequest("Client A", List.of(itemRequest));

        // Act
        Order domain = mapper.toDomainCommand(request);

        // Assert
        assertEquals("Client A", domain.getCustomerName());
        assertEquals(1, domain.getItems().size());
        assertEquals("prod-1", domain.getItems().get(0).getProductId());
        
        // Fields with @Mapping(ignore = true) should be null
        assertNull(domain.getId());
        assertNull(domain.getStatus());
        assertNull(domain.getTotalAmount());
        assertNull(domain.getCreatedAt());
        assertNull(domain.getUpdatedAt());
    }

    @Test
    void shouldMapDomainToResponseDto() {
        // Arrange
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        OrderItem item = OrderItem.builder()
                .productId("p1")
                .productName("Item 1")
                .quantity(5)
                .unitPrice(new BigDecimal("2.00"))
                .build();
        
        Order order = Order.builder()
                .id(id)
                .customerName("Bob")
                .status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("10.00"))
                .createdAt(now)
                .items(List.of(item))
                .build();

        // Act
        OrderDTOs.OrderResponse response = mapper.toResponseDto(order);

        // Assert
        assertEquals(id.toString(), response.id());
        assertEquals("Bob", response.customerName());
        assertEquals("CONFIRMED", response.status());
        assertEquals(new BigDecimal("10.00"), response.totalAmount());
        assertNotNull(response.createdAt());
        assertEquals(1, response.items().size());
    }
}
