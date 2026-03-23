package com.oms.domain.model;

import com.oms.domain.exception.InvalidOrderException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entidad 'Order' y Aggregate Root de nuestro dominio de gestión de pedidos.
 * Como Aggregate Root, es el único objeto del dominio con el cual los adaptadores interactúan.
 * No tiene anotaciones de frameworks como '@Entity'. Todo el estado se maneja con constructores o métodos lógicos puro Java.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    private UUID id;
    private String customerName;
    private List<OrderItem> items;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;

    /**
     * Comportamiento del dominio: calcular el total de la orden basado en los ítems
     * y obligar al estado PENDING a la hora de inicializar/validar un pedido entrante.
     * <p>
     * Aquí no utilizamos "Setters" estándar. Toda mutación del estado pasa por 
     * métodos explícitos del dominio que aseguran coherencia. (ejemplo: updateStatus())
     */
    public void validateAndInitialize() {
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new InvalidOrderException("Customer name cannot be empty");
        }
        if (items == null || items.isEmpty()) {
            throw new InvalidOrderException("Order must have at least one item");
        }

        this.status = OrderStatus.PENDING;
        
        // Sumamos todos los subtotales usando Streams de Java
        this.totalAmount = items.stream()
                .map(OrderItem::calculateSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Asignamos un ID si no hay uno (esto será valioso más adelante, 
        // ya que nuestro JPA repository tratará de persistir este ID ya definido)
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    /**
     * Mutador de comportamiento (No usamos Setters crudos)
     * En DDD (Domain Driven Design), este tipo de métodos encapsulan la progresión
     * del estado de la orden (máquina de estados).
     */
    public void updateStatus(OrderStatus newStatus) {
        if (this.status == OrderStatus.CANCELLED) {
            throw new InvalidOrderException("Cannot change status of a cancelled order");
        }
        if (this.status == OrderStatus.DELIVERED && newStatus != OrderStatus.DELIVERED) {
             throw new InvalidOrderException("Cannot change status as the order is already delivered");
        }
        this.status = newStatus;
    }
}
