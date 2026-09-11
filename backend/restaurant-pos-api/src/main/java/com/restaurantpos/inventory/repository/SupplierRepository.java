package com.restaurantpos.inventory.repository;

import com.restaurantpos.inventory.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    List<Supplier> findByTenantIdAndDeletedAtIsNullOrderByNameAsc(UUID tenantId);
    Optional<Supplier> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
}
