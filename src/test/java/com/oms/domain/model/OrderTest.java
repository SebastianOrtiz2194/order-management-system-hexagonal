package com.oms.domain.model;

import com.oms.domain.exception.InvalidOrderException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void shouldSuccessfullyInitializeValidOrder() {
        // Arrange
        OrderItem item = OrderItem.builder()
                .productId("sku-123")
                .quantity(2)
                .unitPrice(new BigDecimal("50.00"))
                .build();
        
        Order order = Order.builder()
                .customerName("John Doe")
                .items(List.of(item))
                .build();

        // Act
        order.validateAndInitialize();

        // Assert
        assertNotNull(order.getId(), "La clave 'id' debería crearse si es nula");
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(new BigDecimal("100.00"), order.getTotalAmount());
    }

    @Test
    void shouldThrowExceptionWhenNoItems() {
        // Arrange
        Order orderWithEmptyItems = Order.builder()
                .customerName("Jane Doe")
                .items(Collections.emptyList())
                .build();
        
        Order orderWithNullItems = Order.builder()
                .customerName("Jake Doe")
                .items(null)
                .build();

        // Act & Assert
        Exception e1 = assertThrows(InvalidOrderException.class, orderWithEmptyItems::validateAndInitialize);
        assertEquals("Order must have at least one item", e1.getMessage());

        assertThrows(InvalidOrderException.class, orderWithNullItems::validateAndInitialize);
    }
    
    @Test
    void shouldThrowExceptionOnEmptyCustomerName() {
        // Arrange
        Order emptyNameOrder = Order.builder()
                .customerName("  ")
                .items(List.of(OrderItem.builder().build())) // items dummy
                .build();

        // Act & Assert
        assertThrows(InvalidOrderException.class, emptyNameOrder::validateAndInitialize);
    }
}
