package com.oms.application.service;

import com.oms.application.port.output.OrderEventPublisherPort;
import com.oms.application.port.output.OrderRepositoryPort;
import com.oms.domain.event.OrderCreatedEvent;
import com.oms.domain.model.Order;
import com.oms.domain.model.OrderItem;
import com.oms.domain.model.OrderStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
//import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Probando Service Application sin cargar contexto Spring. Usamos Mockito para
 * "aislar"
 * el puerto de salida a Base de Datos de JPA real y simular la publicación en
 * Kafka
 */
@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private OrderEventPublisherPort eventPublisher;

    @InjectMocks
    private CreateOrderService createOrderService;

    private Order dummyOrder;

    @BeforeEach
    void setUp() {
        OrderItem item = OrderItem.builder().productId("1").quantity(1).unitPrice(BigDecimal.TEN).build();
        dummyOrder = Order.builder()
                .customerName("Bob")
                .items(List.of(item))
                .build();
    }

    @Test
    void createOrderSuccessFlow() {
        // Simulate DB Persistence Behavior (simulamos que save() siempre devuelve
        // nuestro objeto mockeado)
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = createOrderService.createOrder(dummyOrder);

        // Verificamos inicialización de Dominio (si falló 'validateAndInitialize' esto
        // explotaba antes)
        assertNotNull(result.getId());
        assertEquals(OrderStatus.PENDING, result.getStatus());

        // Verificamos interacción del Adaptador (Llamada Obligatoria 'save()')
        verify(orderRepository, times(1)).save(any(Order.class));

        // Verificamos Publicación del Evento
        verify(eventPublisher, times(1)).publish(any(OrderCreatedEvent.class));
    }
}
