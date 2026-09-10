package com.restaurantpos.tables.repository;

import com.restaurantpos.tables.entity.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, UUID> {

    List<RestaurantTable> findByTenantIdAndDeletedAtIsNullOrderByTableNumberAsc(UUID tenantId);

    List<RestaurantTable> findByTenantIdAndZoneIdAndDeletedAtIsNullOrderByTableNumberAsc(UUID tenantId, UUID zoneId);

    Optional<RestaurantTable> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<RestaurantTable> findByTenantIdAndTableNumberAndDeletedAtIsNull(UUID tenantId, String tableNumber);
}
