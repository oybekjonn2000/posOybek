package com.restaurantpos.orders.repository;

import com.restaurantpos.orders.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderIdOrderBySortOrderAsc(UUID orderId);

    List<OrderItem> findByKitchenStatusInAndVoidedFalseOrderByCreatedAtAsc(List<OrderItem.KitchenStatus> statuses);

    long countByKitchenId(UUID kitchenId);
}
