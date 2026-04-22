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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CreateOrderService. 
 * Operates by testing the application service without loading the full Spring context. 
 * Mocks are used to isolate the output ports (Persistence and Messaging).
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

    /**
     * Verifies the successful creation flow: validation, persistence, and event publication.
     */
    @Test
    void createOrderSuccessFlow() {
        // Arrange: Simulate persistence behavior where save() returns the provided object.
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order result = createOrderService.createOrder(dummyOrder);

        // Assert: Domain initialization (id assignment and initial PENDING status).
        assertNotNull(result.getId());
        assertEquals(OrderStatus.PENDING, result.getStatus());

        // Assert: Interaction with the persistence adapter.
        verify(orderRepository, times(1)).save(any(Order.class));

        // Assert: Interaction with the event publisher.
        verify(eventPublisher, times(1)).publish(any(OrderCreatedEvent.class));
    }
}
