package com.restaurantpos.inventory.repository;

import com.restaurantpos.inventory.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {

    List<InventoryTransaction> findByItemIdOrderByCreatedAtDesc(UUID itemId, Pageable pageable);

    List<InventoryTransaction> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    boolean existsByReferenceTypeAndReferenceIdAndType(String referenceType, UUID referenceId, InventoryTransaction.TransactionType type);

    @Query("SELECT t FROM InventoryTransaction t WHERE t.tenant.id = :tenantId " +
           "AND (:itemId IS NULL OR t.item.id = :itemId) " +
           "AND (:type IS NULL OR t.type = :type) " +
           "AND (:warehouseId IS NULL OR (t.warehouse IS NOT NULL AND t.warehouse.id = :warehouseId)) " +
           "AND (cast(:from as timestamp) IS NULL OR t.createdAt >= :from) " +
           "AND (cast(:to as timestamp) IS NULL OR t.createdAt <= :to) " +
           "ORDER BY t.createdAt DESC")
    List<InventoryTransaction> findFiltered(
            @Param("tenantId") UUID tenantId,
            @Param("itemId") UUID itemId,
            @Param("type") InventoryTransaction.TransactionType type,
            @Param("warehouseId") UUID warehouseId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("SELECT t FROM InventoryTransaction t WHERE t.tenant.id = :tenantId " +
           "AND t.createdAt >= :from AND t.createdAt < :to")
    List<InventoryTransaction> findByTenantAndDateRange(
            @Param("tenantId") UUID tenantId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
