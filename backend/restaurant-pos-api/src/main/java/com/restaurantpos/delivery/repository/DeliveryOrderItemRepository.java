package com.restaurantpos.delivery.repository;

import com.restaurantpos.delivery.entity.DeliveryOrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeliveryOrderItemRepository extends JpaRepository<DeliveryOrderItemEntity, UUID> {
    List<DeliveryOrderItemEntity> findByDeliveryOrderId(UUID deliveryOrderId);
}
