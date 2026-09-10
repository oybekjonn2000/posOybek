package com.restaurantpos.orders.repository;

import com.restaurantpos.orders.entity.CancellationReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CancellationReceiptRepository extends JpaRepository<CancellationReceipt, UUID> {
    List<CancellationReceipt> findByTenantIdAndOrderIdOrderByCreatedAtDesc(UUID tenantId, UUID orderId);
    List<CancellationReceipt> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
