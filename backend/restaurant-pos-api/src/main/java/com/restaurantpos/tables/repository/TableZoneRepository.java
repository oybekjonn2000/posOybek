package com.restaurantpos.tables.repository;

import com.restaurantpos.tables.entity.TableZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TableZoneRepository extends JpaRepository<TableZone, UUID> {

    List<TableZone> findByTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(UUID tenantId);

    java.util.Optional<TableZone> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    java.util.Optional<TableZone> findByTenantIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID tenantId, String name);
}
