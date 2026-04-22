package com.oms.infrastructure.adapter.output.persistence;

import com.oms.domain.model.Order;
import com.oms.domain.model.OrderItem;
import com.oms.domain.model.OrderStatus;
import org.mapstruct.*;

import java.util.List;

/**
 * Compile-time mapper responsible for converting rich domain models into 
 * simple persistence entities and vice versa.
 */
@Mapper(componentModel = "spring")
public interface OrderPersistenceMapper {

    // ==== Domain to JPA Mapping ====
    
    /**
     * Converts a Domain Order into a JPA Entity, ensuring circular references 
     * are correctly established for Hibernate.
     */
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

    // ==== JPA to Domain Mapping ====
    
    @Mapping(source = "status", target = "status", qualifiedByName = "mapStatus")
    Order toDomainModel(OrderJpaEntity jpaEntity);

    @Mapping(target = "productId", source = "productId")
    OrderItem toDomainItem(OrderItemJpaEntity jpaItem);

    /**
     * MapStruct named mapping for status conversion.
     */
    @Named("mapStatus")
    default OrderStatus mapStatus(String status) {
        return status != null ? OrderStatus.valueOf(status) : OrderStatus.PENDING;
    }

    List<Order> toDomainList(List<OrderJpaEntity> jpaEntities);
}
