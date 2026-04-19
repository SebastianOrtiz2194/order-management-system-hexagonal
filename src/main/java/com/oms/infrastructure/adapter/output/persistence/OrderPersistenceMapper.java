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
    default OrderJpaEntity toJpaEntity(Order order) {
        OrderJpaEntity entity = toJpaEntityInternal(order);
        if (entity != null && entity.getItems() != null) {
            entity.getItems().forEach(item -> item.setOrder(entity));
        }
        return entity;
    }

    OrderJpaEntity toJpaEntityInternal(Order order);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    OrderItemJpaEntity toJpaItem(OrderItem item);

    // ==== De JPA a DOMINIO ====
    @Mapping(source = "status", target = "status", qualifiedByName = "mapStatus")
    Order toDomainModel(OrderJpaEntity jpaEntity);

    OrderItem toDomainItem(OrderItemJpaEntity jpaItem);

    @Named("mapStatus")
    default OrderStatus mapStatus(String status) {
        return status != null ? OrderStatus.valueOf(status) : OrderStatus.PENDING;
    }

    List<Order> toDomainList(List<OrderJpaEntity> jpaEntities);
}
