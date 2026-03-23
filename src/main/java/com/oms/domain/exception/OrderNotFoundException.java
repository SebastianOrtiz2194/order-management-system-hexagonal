package com.oms.domain.exception;

/**
 * Excepción de Dominio pura que indica que el identificador buscado no concuerda
 * con un recurso existente. Un Adapter de infraestructura (ej. un ErrorHandler de Spring)
 * podrá después mapear esto a un '404 Not Found'.
 */
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) {
        super(message);
    }
}
