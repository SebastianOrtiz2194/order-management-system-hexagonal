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
 * The 'Order' entity and Aggregate Root for our order management domain.
 * As an Aggregate Root, it acts as the single point of interaction for the domain model.
 * It is free from framework annotations like '@Entity'. State manipulation is strictly 
 * controlled through constructors and pure Java behavior methods to ensure consistency.
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
    private LocalDateTime updatedAt;

    /**
     * Domain behavior: Calculates the total amount of the order based on its items
     * and forces the initial state to PENDING when validating an incoming order request.
     * <p>
     * We avoid using standard "Setters" here. Any state mutation goes through 
     * explicit domain methods that enforce consistency and business rules (e.g., updateStatus()).
     */
    public void validateAndInitialize() {
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new InvalidOrderException("Customer name cannot be empty");
        }
        if (items == null || items.isEmpty()) {
            throw new InvalidOrderException("Order must have at least one item");
        }

        this.status = OrderStatus.PENDING;
        
        // Sum all item subtotals using Java Streams
        this.totalAmount = items.stream()
                .map(OrderItem::calculateSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Assign a default ID if none exists. This proves valuable when the 
        // JPA repository attempts to persist the entity by treating it as a new record.
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Behavior mutator (Raw setters are not used).
     * In Domain-Driven Design (DDD), these types of methods encapsulate the 
     * progression of the order's state (acting like a finite state machine).
     *
     * @param newStatus The target state for the order transition.
     * @throws InvalidOrderException if the transition is explicitly prohibited.
     */
    public void updateStatus(OrderStatus newStatus) {
        if (newStatus == null) {
            throw new InvalidOrderException("New status cannot be null");
        }
        if (!this.status.canTransitionTo(newStatus)) {
            throw new InvalidOrderException(String.format("Invalid status transition from %s to %s", this.status, newStatus));
        }
        
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }
}
