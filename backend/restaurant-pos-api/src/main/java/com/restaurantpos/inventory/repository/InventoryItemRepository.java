package com.restaurantpos.inventory.repository;

import com.restaurantpos.inventory.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    List<InventoryItem> findByTenantIdAndDeletedAtIsNullOrderByNameAsc(UUID tenantId);

    List<InventoryItem> findByTenantIdAndWarehouseIdAndDeletedAtIsNullOrderByNameAsc(UUID tenantId, UUID warehouseId);

    Optional<InventoryItem> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    @Query("SELECT i FROM InventoryItem i WHERE i.tenant.id = :tenantId " +
           "AND i.deletedAt IS NULL AND i.quantity <= i.minQuantity AND i.active = true")
    List<InventoryItem> findLowStockItems(UUID tenantId);

    @Query("SELECT i FROM InventoryItem i WHERE i.tenant.id = :tenantId " +
           "AND i.deletedAt IS NULL " +
           "AND (:warehouseId IS NULL OR (i.warehouse IS NOT NULL AND i.warehouse.id = :warehouseId)) " +
           "AND (:category IS NULL OR i.category = :category) " +
           "AND (:lowStock IS NULL OR (:lowStock = true AND i.quantity <= i.minQuantity) OR (:lowStock = false AND i.quantity > i.minQuantity)) " +
           "ORDER BY i.name ASC")
    List<InventoryItem> findFiltered(
            @Param("tenantId") UUID tenantId,
            @Param("warehouseId") UUID warehouseId,
            @Param("category") String category,
            @Param("lowStock") Boolean lowStock);

    boolean existsByTenantIdAndSkuAndDeletedAtIsNull(UUID tenantId, String sku);
}

