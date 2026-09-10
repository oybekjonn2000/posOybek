package com.restaurantpos.inventory.repository;

import com.restaurantpos.inventory.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {

    List<InventoryTransaction> findByItemIdOrderByCreatedAtDesc(UUID itemId, Pageable pageable);

    List<InventoryTransaction> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
}
