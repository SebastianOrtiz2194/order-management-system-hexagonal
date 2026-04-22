package com.oms.infrastructure.adapter.output.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Anemic JPA Entity used exclusively by Hibernate/JPA for database mapping.
 * Its sole responsibility is to map class fields to PostgreSQL columns.
 * It must remain devoid of business logic.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor // Required by JPA specification
@AllArgsConstructor
@Builder
public class OrderJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // One-to-Many relationship in the relational database model
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<OrderItemJpaEntity> items;

    /**
     * JPA Callback to automatically assign the creation timestamp before database insertion.
     */
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
