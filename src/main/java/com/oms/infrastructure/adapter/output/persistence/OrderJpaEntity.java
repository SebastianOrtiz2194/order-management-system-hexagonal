package com.oms.infrastructure.adapter.output.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entidad 'Tonta' (Anémica) utilizada exclusivamente por Hibernate/JPA.
 * Su única responsabilidad es mapear columnas en PostgreSQL. No debe tener lógica de negocio.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor // Requerido por JPA
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

    // Relación One-To-Many en Base de datos relacional
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<OrderItemJpaEntity> items;

    // JPA Callbacks para asignar fechas de creación automáticamente antes de la inserción en BD
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
