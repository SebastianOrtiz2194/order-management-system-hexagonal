package com.oms.domain.model;

import java.util.Set;

/**
 * Pure domain enumeration denoting the lifecycle stages of an order.
 * Operates as a Finite State Machine (FSM) dictating allowed transitions.
 */
public enum OrderStatus {
    DELIVERED(Set.of()),
    CANCELLED(Set.of()),
    SHIPPED(Set.of(DELIVERED, CANCELLED)),
    CONFIRMED(Set.of(SHIPPED, CANCELLED)),
    PENDING(Set.of(CONFIRMED, CANCELLED));

    private final Set<OrderStatus> validTransitions;

    OrderStatus(Set<OrderStatus> validTransitions) {
        this.validTransitions = validTransitions;
    }

    /**
     * Validates if the current state is permitted to transition to the requested target state.
     *
     * @param targetStatus The desired new state.
     * @return true if the transition is explicitly allowed, otherwise false.
     */
    public boolean canTransitionTo(OrderStatus targetStatus) {
        return this.validTransitions.contains(targetStatus);
    }
}
