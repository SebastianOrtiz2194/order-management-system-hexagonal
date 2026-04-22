package com.oms.application.port.output;

import com.oms.domain.event.OrderCreatedEvent;

/**
 * Output Port: Event Notification Requirement.
 * "Notify external systems that an event occurred" -> Produces asynchronous events.
 * It is completely decoupled from messaging brokers like Kafka or RabbitMQ.
 */
public interface OrderEventPublisherPort {
    void publish(OrderCreatedEvent event);
}
