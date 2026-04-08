package com.oms.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {

    @Test
    void testValidTransitionsFromPending() {
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CONFIRMED));
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED));

        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.SHIPPED));
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.DELIVERED));
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.PENDING));
    }

    @Test
    void testValidTransitionsFromConfirmed() {
        assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.SHIPPED));
        assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CANCELLED));

        assertFalse(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.PENDING));
        assertFalse(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.DELIVERED));
        assertFalse(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CONFIRMED));
    }

    @Test
    void testValidTransitionsFromShipped() {
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED));
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CANCELLED));

        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.PENDING));
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CONFIRMED));
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.SHIPPED));
    }

    @Test
    void testTransitionsFromDeliveredAndCancelled() {
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.PENDING));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CONFIRMED));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.SHIPPED));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CANCELLED));

        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PENDING));
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CONFIRMED));
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.SHIPPED));
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.DELIVERED));
    }
}
