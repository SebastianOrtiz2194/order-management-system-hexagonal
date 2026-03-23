package com.oms.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
//import java.util.UUID;

/**
 * Value Object 'OrderItem'.
 * Representa la línea de un pedido. Como Value Object, no necesita un ID propio
 * dentro del dominio.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    private String productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;

    /**
     * Comportamiento estático o inmutable:
     * Calcula dinámicamente el precio total del ítem (precio unitario * cantidad).
     */
    public BigDecimal calculateSubtotal() {
        if (unitPrice == null || quantity <= 0) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
