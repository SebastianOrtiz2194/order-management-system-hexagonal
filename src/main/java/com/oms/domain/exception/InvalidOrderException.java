package com.oms.domain.exception;

/**
 * Pure domain business rule exception.
 * This exception is completely decoupled from database constraints or Spring Web components.
 * It is thrown when an order fails domain validation or state transition rules.
 */
public class InvalidOrderException extends RuntimeException {
    public InvalidOrderException(String message) {
        super(message);
    }
}
