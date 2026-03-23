package com.oms.application.port.input;

import com.oms.domain.model.Order;

/**
 * Caso de Uso (Input Port): "El sistema debe permitir que se cree una Orden".
 * Esta abstracción de entrada define lo que la aplicación puede hacer. Un adaptador 
 * externo, como un Spring '@RestController' (incluso una CLI o test), invocará esta ruta.
 */
public interface CreateOrderUseCase {
    Order createOrder(Order orderCommand);
}
