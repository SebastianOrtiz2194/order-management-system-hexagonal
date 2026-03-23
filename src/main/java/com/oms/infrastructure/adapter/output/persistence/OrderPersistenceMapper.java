package com.oms.infrastructure.adapter.output.persistence;

import com.oms.domain.model.Order;
import com.oms.domain.model.OrderItem;
import com.oms.domain.model.OrderStatus;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper en tiempo de compilación. Convierte el objeto riquísimo en negocio (Dominio) 
 * en una entidad de tabla simple (Persistencia) y viceversa.
 */
@Mapper(componentModel = "spring")
public interface OrderPersistenceMapper {

    // ==== De DOMINIO a JPA ====
    // Ignoramos createdAt si lo va a crear @PrePersist.
    @Mapping(target = "createdAt", ignore = true)
    OrderJpaEntity toJpaEntity(Order order);

    @Mapping(target = "id", ignore = true) // El VO no tiene ID, JPA se encarga de autogenerarlo.
    @Mapping(target = "order", ignore = true) // Ciclamos después para re-vincular
    OrderItemJpaEntity toJpaItem(OrderItem item);

    // Método hook para forzar que los ítem anémicos apunten al padre (necesario por hibernate)
    @AfterMapping
    default void linkOrderItems(@MappingTarget OrderJpaEntity orderJpaEntity) {
        if (orderJpaEntity.getItems() != null) {
            orderJpaEntity.getItems().forEach(item -> item.setOrder(orderJpaEntity));
        }
    }

    // ==== De JPA a DOMINIO ====
    @Mapping(source = "status", target = "status", qualifiedByName = "mapStatus")
    Order toDomainModel(OrderJpaEntity jpaEntity);

    OrderItem toDomainItem(OrderItemJpaEntity jpaItem);

    // Parseo de enum
    @Named("mapStatus")
    default OrderStatus mapStatus(String status) {
        return status != null ? OrderStatus.valueOf(status) : OrderStatus.PENDING;
    }

    List<Order> toDomainList(List<OrderJpaEntity> jpaEntities);
}
