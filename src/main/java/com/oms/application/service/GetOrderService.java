package com.oms.application.service;

import com.oms.application.port.input.GetOrderUseCase;
import com.oms.application.port.input.UpdateOrderStatusUseCase;
import com.oms.application.port.output.OrderCachePort;
import com.oms.application.port.output.OrderRepositoryPort;
import com.oms.domain.exception.OrderNotFoundException;
import com.oms.domain.model.Order;
import com.oms.domain.model.OrderStatus;
import com.oms.domain.model.PagedResult;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Orquesta la búsqueda y actualizaciones de la Orden.
 * Implementa un Patrón 'Cache-Aside': Consultamos Redis si no existe, 
 * buscamos en Base Relacional y popularizamos nuevamente la Caché.
 */
@RequiredArgsConstructor
public class GetOrderService implements GetOrderUseCase, UpdateOrderStatusUseCase {

    private final OrderRepositoryPort orderRepository;
    private final OrderCachePort orderCachePort;

    @Override
    public Order getOrderById(UUID id) {
        // 1. Buscar primero en Redis Cache
        return orderCachePort.findById(id)
                .orElseGet(() -> {
                    // 2. Fallback: Buscar en DB (JPA Postgres)
                    Order orderFromDb = orderRepository.findById(id)
                            .orElseThrow(() -> new OrderNotFoundException("Order with ID " + id + " not found."));
                    
                    // 3. Llenamos el caché local pasándolo a Redis
                    orderCachePort.save(orderFromDb);
                    return orderFromDb;
                });
    }

    @Override
    public Order updateStatus(UUID id, OrderStatus newStatus) {
        // Obtiene del core (usa nuestra función ya programada)
        Order order = this.getOrderById(id);
        
        // Delega lógica puramente transaccional al CORE de negocio (Evita lógica Anémica)
        order.updateStatus(newStatus);
        
        // Persistir en Base Relacional y Evict Caché sucio (Evita Stale Data)
        Order saved = orderRepository.save(order);
        orderCachePort.evict(id);
        
        return saved;
    }

    @Override
    public PagedResult<Order> getAllOrders(int page, int size, OrderStatus status) {
        return orderRepository.findAll(page, size, status);
    }
}
