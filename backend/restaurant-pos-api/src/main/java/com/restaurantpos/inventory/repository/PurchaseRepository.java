package com.restaurantpos.inventory.repository;

import com.restaurantpos.inventory.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {
    List<Purchase> findByTenantIdAndDeletedAtIsNullOrderByPurchaseDateDesc(UUID tenantId);
    Optional<Purchase> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
    List<Purchase> findByTenantIdAndPurchaseDateBetweenAndDeletedAtIsNull(UUID tenantId, Instant from, Instant to);
    List<Purchase> findByTenantIdAndStatusAndPurchaseDateBetweenAndDeletedAtIsNull(UUID tenantId, Purchase.PurchaseStatus status, Instant from, Instant to);
}
