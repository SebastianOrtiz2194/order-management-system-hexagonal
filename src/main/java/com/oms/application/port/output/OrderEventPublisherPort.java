package com.oms.application.port.output;

import com.oms.domain.event.OrderCreatedEvent;

/**
 * Output Port: "Avisar a sistemas externos que algo sucedió" -> Produce eventos asíncronos.
 */
public interface OrderEventPublisherPort {
    void publish(OrderCreatedEvent event);
}
