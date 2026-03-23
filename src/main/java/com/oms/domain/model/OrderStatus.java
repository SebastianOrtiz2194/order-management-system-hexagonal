package com.oms.domain.model;

/**
 * Enum puro del dominio que representa el ciclo de vida de una orden.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
