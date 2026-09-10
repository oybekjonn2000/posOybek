package com.restaurantpos.inventory.repository;

import com.restaurantpos.inventory.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    List<InventoryItem> findByTenantIdAndDeletedAtIsNullOrderByNameAsc(UUID tenantId);

    Optional<InventoryItem> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    @Query("SELECT i FROM InventoryItem i WHERE i.tenant.id = :tenantId " +
           "AND i.deletedAt IS NULL AND i.quantity <= i.minQuantity AND i.active = true")
    List<InventoryItem> findLowStockItems(UUID tenantId);

    boolean existsByTenantIdAndSkuAndDeletedAtIsNull(UUID tenantId, String sku);
}
