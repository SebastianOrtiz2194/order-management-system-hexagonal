package com.oms.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for OrderStatus enum logic.
 * Validates the state machine transitions defined in the canTransitionTo method.
 */
@DisplayName("OrderStatus — State Machine Logic Tests")
class OrderStatusTest {

    @Test
    @DisplayName("Valid transitions originating from PENDING")
    void testValidTransitionsFromPending() {
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CONFIRMED), "PENDING -> CONFIRMED should be valid.");
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED), "PENDING -> CANCELLED should be valid.");

        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.SHIPPED), "PENDING -> SHIPPED should be invalid.");
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.DELIVERED), "PENDING -> DELIVERED should be invalid.");
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.PENDING), "Self-transitions should be invalid.");
    }

    @Test
    @DisplayName("Valid transitions originating from CONFIRMED")
    void testValidTransitionsFromConfirmed() {
        assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.SHIPPED), "CONFIRMED -> SHIPPED should be valid.");
        assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CANCELLED), "CONFIRMED -> CANCELLED should be valid.");

        assertFalse(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.PENDING), "Reverse transitions to PENDING should be invalid.");
        assertFalse(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.DELIVERED), "Skipping SHIPPED state should be invalid.");
        assertFalse(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CONFIRMED), "Self-transitions should be invalid.");
    }

    @Test
    @DisplayName("Valid transitions originating from SHIPPED")
    void testValidTransitionsFromShipped() {
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED), "SHIPPED -> DELIVERED should be valid.");
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CANCELLED), "SHIPPED -> CANCELLED should be valid (e.g., lost in transit).");

        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.PENDING), "Reverse transitions should be invalid.");
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CONFIRMED), "Reverse transitions should be invalid.");
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.SHIPPED), "Self-transitions should be invalid.");
    }

    @Test
    @DisplayName("Transitions originating from terminal states (DELIVERED, CANCELLED)")
    void testTransitionsFromDeliveredAndCancelled() {
        // DELIVERED is a terminal state.
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.PENDING));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CONFIRMED));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.SHIPPED));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CANCELLED));

        // CANCELLED is a terminal state.
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PENDING));
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CONFIRMED));
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.SHIPPED));
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.DELIVERED));
    }
}
