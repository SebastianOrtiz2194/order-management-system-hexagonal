package com.oms.application.port.input;

import com.oms.domain.model.Order;

/**
 * Use Case (Input Port): "The system must allow an Order to be created."
 * This input abstraction defines operations the application exposes. An external adapter,
 * such as a Spring '@RestController' (or even a CLI or test), will invoke this port 
 * to trigger the business use case.
 */
public interface CreateOrderUseCase {
    Order createOrder(Order orderCommand);
}
