package com.oms.infrastructure.config;

import com.oms.application.port.output.OrderCachePort;
import com.oms.application.port.output.OrderEventPublisherPort;
import com.oms.application.port.output.OrderRepositoryPort;
import com.oms.application.service.CreateOrderService;
import com.oms.application.service.GetOrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Lugar Unificado de Inyección de Dependencias.
 * Esto "levanta" las clases puras en un contenedor Spring.
 */
@Configuration
public class BeanConfig {

    @Bean
    public CreateOrderService createOrderService(
            OrderRepositoryPort orderRepositoryPort,
            OrderEventPublisherPort orderEventPublisherPort) {
        return new CreateOrderService(orderRepositoryPort, orderEventPublisherPort);
    }

    @Bean
    public GetOrderService getOrderService(
            OrderRepositoryPort orderRepositoryPort,
            OrderCachePort orderCachePort) {
        return new GetOrderService(orderRepositoryPort, orderCachePort);
    }
}
