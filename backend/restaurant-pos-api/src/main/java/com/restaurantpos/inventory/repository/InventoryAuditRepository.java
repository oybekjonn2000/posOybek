package com.restaurantpos.inventory.repository;

import com.restaurantpos.inventory.entity.InventoryAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryAuditRepository extends JpaRepository<InventoryAudit, UUID> {
    List<InventoryAudit> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<InventoryAudit> findByIdAndTenantId(UUID id, UUID tenantId);
}
