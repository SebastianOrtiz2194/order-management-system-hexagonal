package com.oms.domain.exception;

/**
 * Pure domain exception indicating that an entity with the requested identifier could not be found.
 * An infrastructure adapter (e.g., a Spring GlobalExceptionHandler) can later intercept this 
 * and map it to an appropriate HTTP 404 Not Found response.
 */
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) {
        super(message);
    }
}
