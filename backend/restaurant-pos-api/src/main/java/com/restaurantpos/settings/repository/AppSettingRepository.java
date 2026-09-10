package com.restaurantpos.settings.repository;

import com.restaurantpos.settings.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppSettingRepository extends JpaRepository<AppSetting, UUID> {

    List<AppSetting> findByTenantId(UUID tenantId);

    List<AppSetting> findByTenantIdAndCategory(UUID tenantId, String category);

    Optional<AppSetting> findByTenantIdAndCategoryAndKey(UUID tenantId, String category, String key);
}
