package com.restaurantpos.orders.repository;

import com.restaurantpos.orders.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByTenantIdAndDeletedAtIsNullOrderByOpenedAtDesc(UUID tenantId);

    List<Order> findByTenantIdAndStatusInAndDeletedAtIsNullOrderByOpenedAtDesc(UUID tenantId, List<Order.OrderStatus> statuses);

    Optional<Order> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<Order> findByTenantIdAndOrderNumber(UUID tenantId, String orderNumber);

    Optional<Order> findByTableIdAndStatusInAndDeletedAtIsNull(UUID tableId, List<Order.OrderStatus> statuses);

    List<Order.OrderStatus> ACTIVE_STATUSES = List.of(
            Order.OrderStatus.OPEN,
            Order.OrderStatus.IN_PROGRESS,
            Order.OrderStatus.READY
    );

    default List<Order> findActiveOrders(UUID tenantId) {
        return findByTenantIdAndStatusInAndDeletedAtIsNullOrderByOpenedAtDesc(tenantId, ACTIVE_STATUSES);
    }
}
