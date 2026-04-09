package com.oms.application.service;

import com.oms.application.port.output.OrderCachePort;
import com.oms.application.port.output.OrderRepositoryPort;
import com.oms.domain.exception.OrderNotFoundException;
import com.oms.domain.model.Order;
import com.oms.domain.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetOrderServiceTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private OrderCachePort orderCachePort;

    @InjectMocks
    private GetOrderService getOrderService;

    private UUID orderId;
    private Order dummyOrder;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        dummyOrder = Order.builder()
                .id(orderId)
                .customerName("Alice")
                .status(OrderStatus.PENDING)
                .build();
    }

    @Test
    void shouldReturnFromCacheWhenAvailable() {
        // Arrange
        when(orderCachePort.findById(orderId)).thenReturn(Optional.of(dummyOrder));

        // Act
        Order result = getOrderService.getOrderById(orderId);

        // Assert
        assertEquals(dummyOrder, result);
        verify(orderCachePort, times(1)).findById(orderId);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void shouldReturnFromRepoAndSaveInCacheWhenCacheMiss() {
        // Arrange
        when(orderCachePort.findById(orderId)).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(dummyOrder));

        // Act
        Order result = getOrderService.getOrderById(orderId);

        // Assert
        assertEquals(dummyOrder, result);
        verify(orderCachePort, times(1)).findById(orderId);
        verify(orderRepository, times(1)).findById(orderId);
        verify(orderCachePort, times(1)).save(dummyOrder);
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFoundInBoth() {
        // Arrange
        when(orderCachePort.findById(orderId)).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(OrderNotFoundException.class, () -> getOrderService.getOrderById(orderId));
    }

    @Test
    void shouldUpdateStatusAndEvictCache() {
        // Arrange
        when(orderCachePort.findById(orderId)).thenReturn(Optional.of(dummyOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Order result = getOrderService.updateStatus(orderId, OrderStatus.CONFIRMED);

        // Assert
        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderCachePort, times(1)).evict(orderId);
    }

    @Test
    void shouldReturnAllOrdersFromRepo() {
        // Arrange
        List<Order> orders = List.of(dummyOrder);
        when(orderRepository.findAll()).thenReturn(orders);

        // Act
        List<Order> result = getOrderService.getAllOrders();

        // Assert
        assertEquals(1, result.size());
        assertEquals(dummyOrder, result.get(0));
        verify(orderRepository, times(1)).findAll();
    }
}
