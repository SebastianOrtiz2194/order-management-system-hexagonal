package com.oms.domain.exception;

/**
 * Excepción de regla de Negocio pura, no ligada a bases de datos ni Spring Web.
 */
public class InvalidOrderException extends RuntimeException {
    public InvalidOrderException(String message) {
        super(message);
    }
}
