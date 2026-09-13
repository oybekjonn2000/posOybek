package com.restaurantpos.orders.repository;

import com.restaurantpos.orders.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByTenantIdAndDeletedAtIsNullOrderByOpenedAtDesc(UUID tenantId);

    List<Order> findByTenantIdAndStatusInAndDeletedAtIsNullOrderByOpenedAtDesc(UUID tenantId, List<Order.OrderStatus> statuses);

    Optional<Order> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.tenant.id = :tenantId AND o.deletedAt IS NULL")
    Optional<Order> findByIdWithLock(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    Optional<Order> findByTenantIdAndOrderNumber(UUID tenantId, String orderNumber);

    Optional<Order> findByTableIdAndStatusInAndDeletedAtIsNull(UUID tableId, List<Order.OrderStatus> statuses);

    List<Order.OrderStatus> ACTIVE_STATUSES = List.of(
            Order.OrderStatus.OPEN,
            Order.OrderStatus.IN_PROGRESS,
            Order.OrderStatus.READY
    );

    List<Order.OrderStatus> HISTORY_STATUSES = List.of(
            Order.OrderStatus.PAID
    );

    default List<Order> findActiveOrders(UUID tenantId) {
        return findByTenantIdAndStatusInAndDeletedAtIsNullOrderByOpenedAtDesc(tenantId, ACTIVE_STATUSES);
    }

    default List<Order> findHistoryOrders(UUID tenantId) {
        return findByTenantIdAndStatusInAndDeletedAtIsNullOrderByOpenedAtDesc(tenantId, HISTORY_STATUSES);
    }

    List<Order> findByTenantIdAndStatusInAndWaiterIdAndDeletedAtIsNullOrderByOpenedAtDesc(UUID tenantId, List<Order.OrderStatus> statuses, UUID waiterId);

    default List<Order> findActiveOrdersByWaiter(UUID tenantId, UUID waiterId) {
        return findByTenantIdAndStatusInAndWaiterIdAndDeletedAtIsNullOrderByOpenedAtDesc(tenantId, ACTIVE_STATUSES, waiterId);
    }

    default List<Order> findHistoryOrdersByWaiter(UUID tenantId, UUID waiterId) {
        return findByTenantIdAndStatusInAndWaiterIdAndDeletedAtIsNullOrderByOpenedAtDesc(tenantId, HISTORY_STATUSES, waiterId);
    }

    org.springframework.data.domain.Page<Order> findByTenantIdAndStatusInAndDeletedAtIsNullOrderByOpenedAtDesc(
            UUID tenantId, List<Order.OrderStatus> statuses, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Order> findByTenantIdAndStatusInAndWaiterIdAndDeletedAtIsNullOrderByOpenedAtDesc(
            UUID tenantId, List<Order.OrderStatus> statuses, UUID waiterId, org.springframework.data.domain.Pageable pageable);

    default org.springframework.data.domain.Page<Order> findActiveOrders(UUID tenantId, org.springframework.data.domain.Pageable pageable) {
        return findByTenantIdAndStatusInAndDeletedAtIsNullOrderByOpenedAtDesc(tenantId, ACTIVE_STATUSES, pageable);
    }

    default org.springframework.data.domain.Page<Order> findActiveOrdersByWaiter(UUID tenantId, UUID waiterId, org.springframework.data.domain.Pageable pageable) {
        return findByTenantIdAndStatusInAndWaiterIdAndDeletedAtIsNullOrderByOpenedAtDesc(tenantId, ACTIVE_STATUSES, waiterId, pageable);
    }

    default org.springframework.data.domain.Page<Order> findHistoryOrders(UUID tenantId, org.springframework.data.domain.Pageable pageable) {
        return findByTenantIdAndStatusInAndDeletedAtIsNullOrderByOpenedAtDesc(tenantId, HISTORY_STATUSES, pageable);
    }

    default org.springframework.data.domain.Page<Order> findHistoryOrdersByWaiter(UUID tenantId, UUID waiterId, org.springframework.data.domain.Pageable pageable) {
        return findByTenantIdAndStatusInAndWaiterIdAndDeletedAtIsNullOrderByOpenedAtDesc(tenantId, HISTORY_STATUSES, waiterId, pageable);
    }

    List<Order> findByTenantIdAndStatusAndPaidAtBetweenAndDeletedAtIsNull(UUID tenantId, Order.OrderStatus status, Instant from, Instant to);

    List<Order> findByTenantIdAndStatusAndPaidAtBetweenAndWaiterIdAndDeletedAtIsNull(UUID tenantId, Order.OrderStatus status, Instant from, Instant to, UUID waiterId);

    List<Order> findByTenantIdAndStatusAndClosedAtBetweenAndDeletedAtIsNull(UUID tenantId, Order.OrderStatus status, Instant from, Instant to);

    List<Order> findByTenantIdAndOpenedAtBetweenAndDeletedAtIsNull(UUID tenantId, Instant from, Instant to);

    long countByTenantIdAndStatusAndOpenedAtBetweenAndDeletedAtIsNull(UUID tenantId, Order.OrderStatus status, Instant from, Instant to);

    long countByTenantIdAndStatusAndClosedAtBetweenAndDeletedAtIsNull(UUID tenantId, Order.OrderStatus status, Instant from, Instant to);
}

