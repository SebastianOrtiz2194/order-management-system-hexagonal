package com.oms.domain.model;

import com.oms.domain.exception.InvalidOrderException;
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
     * Self-validation of the Value Object's invariants.
     * The Order aggregate invokes this during {@link Order#validateAndInitialize()}
     * so that invalid line items fail fast instead of corrupting totals.
     *
     * @throws InvalidOrderException if any invariant is violated.
     */
    public void validate() {
        if (productId == null || productId.trim().isEmpty()) {
            throw new InvalidOrderException("Product ID cannot be blank");
        }
        if (quantity == null || quantity <= 0) {
            throw new InvalidOrderException("Quantity must be strictly positive");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException("Unit price must be strictly positive");
        }
    }

    /**
     * Dynamically computes the total price for this specific item (unit price * quantity).
     * Assumes the item was previously validated; computing a subtotal on an
     * unvalidated item is a programming error and fails loudly instead of
     * silently returning zero.
     *
     * @return The calculated subtotal as a BigDecimal.
     */
    public BigDecimal calculateSubtotal() {
        if (unitPrice == null || quantity == null) {
            throw new IllegalStateException("Cannot calculate subtotal for an unvalidated item");
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
