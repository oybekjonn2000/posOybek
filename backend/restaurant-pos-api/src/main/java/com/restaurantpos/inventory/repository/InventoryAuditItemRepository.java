package com.restaurantpos.inventory.repository;

import com.restaurantpos.inventory.entity.InventoryAuditItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryAuditItemRepository extends JpaRepository<InventoryAuditItem, UUID> {
    List<InventoryAuditItem> findByAuditId(UUID auditId);
}
