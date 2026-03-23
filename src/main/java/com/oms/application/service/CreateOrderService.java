package com.oms.application.service;

import com.oms.application.port.input.CreateOrderUseCase;
import com.oms.application.port.output.OrderEventPublisherPort;
import com.oms.application.port.output.OrderRepositoryPort;
import com.oms.domain.event.OrderCreatedEvent;
import com.oms.domain.model.Order;
import lombok.RequiredArgsConstructor;

/**
 * Orquestador principal de la "Creación de Orden".
 * 
 * Flujo:
 * 1. Llama al dominio puro (validateAndInitialize) para validar negocio.
 * 2. Manda a guardar el objeto en persistencia (adaptador externo lo hará real).
 * 3. Publica un evento de Dominio para que futuros sub-sistemas actúen (Kafka en Fase 5).
 * 
 * NOTA: No usamos '@Service' de Spring aquí para mantener esta capa "Pura".
 */
@RequiredArgsConstructor
public class CreateOrderService implements CreateOrderUseCase {

    private final OrderRepositoryPort orderRepository;
    private final OrderEventPublisherPort eventPublisher;

    @Override
    public Order createOrder(Order orderCommand) {
        // 1. Dominio: Valida reglas y define estado inicial
        orderCommand.validateAndInitialize();
        
        // 2. Persistencia secundaria
        Order savedOrder = orderRepository.save(orderCommand);
        
        // 3. Generación local del Evendo de Dominio Asíncrono
        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getCustomerName(),
                savedOrder.getTotalAmount(),
                savedOrder.getStatus().name()
        );
        eventPublisher.publish(event);
        
        return savedOrder;
    }
}
