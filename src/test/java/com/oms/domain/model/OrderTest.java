package com.oms.domain.model;

import com.oms.domain.exception.InvalidOrderException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Order Aggregate Root.
 * Validates core business rules, state transitions, and initialization logic.
 */
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
        assertNotNull(order.getId(), "The 'id' should be automatically generated if null during initialization.");
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
                .items(List.of(OrderItem.builder().build())) // Dummy item
                .build();

        // Act & Assert
        assertThrows(InvalidOrderException.class, emptyNameOrder::validateAndInitialize);
    }
    
    @Test
    void shouldHandleStatusTransitionsCorrectly() {
        // Arrange
        Order order = Order.builder()
                .customerName("Jane")
                .items(List.of(OrderItem.builder().quantity(1).unitPrice(BigDecimal.TEN).build()))
                .build();
        
        order.validateAndInitialize();
        assertEquals(OrderStatus.PENDING, order.getStatus());
        
        // Act & Assert: Valid PENDING -> CONFIRMED transition
        order.updateStatus(OrderStatus.CONFIRMED);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertNotNull(order.getUpdatedAt());
        
        // Act & Assert: Invalid CONFIRMED -> PENDING transition (regression check)
        assertThrows(InvalidOrderException.class, () -> order.updateStatus(OrderStatus.PENDING));
        
        // Act & Assert: Valid CONFIRMED -> SHIPPED transition
        order.updateStatus(OrderStatus.SHIPPED);
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        
        // Act & Assert: Valid SHIPPED -> DELIVERED transition
        order.updateStatus(OrderStatus.DELIVERED);
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        
        // Act & Assert: Invalid DELIVERED -> CANCELLED transition (terminal state check)
        assertThrows(InvalidOrderException.class, () -> order.updateStatus(OrderStatus.CANCELLED));
    }
}
