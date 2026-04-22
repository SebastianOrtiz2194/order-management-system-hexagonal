package com.oms.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Value Object 'OrderItem'.
 * Represents a single line item within an order. As a Value Object, it does not 
 * inherently require its own unique domain identity.
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
     * Immutable or static behavior block:
     * Dynamically computes the total price for this specific item (unit price * quantity).
     * 
     * @return The calculated subtotal as a BigDecimal.
     */
    public BigDecimal calculateSubtotal() {
        if (unitPrice == null || quantity <= 0) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
