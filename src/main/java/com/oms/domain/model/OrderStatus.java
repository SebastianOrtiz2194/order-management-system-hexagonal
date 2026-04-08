package com.oms.domain.model;

import java.util.Set;

/**
 * Enum puro del dominio que representa el ciclo de vida de una orden
 * y funciona como una máquina de estados finitos (FSM).
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
     * Valida si el estado actual puede transicionar al estado de destino.
     */
    public boolean canTransitionTo(OrderStatus targetStatus) {
        return this.validTransitions.contains(targetStatus);
    }
}
