package com.restaurantpos.users.repository;

import com.restaurantpos.users.entity.EmployeeKitchen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeKitchenRepository extends JpaRepository<EmployeeKitchen, UUID> {
    List<EmployeeKitchen> findByEmployeeId(UUID employeeId);
    List<EmployeeKitchen> findByTenantIdAndEmployeeId(UUID tenantId, UUID employeeId);
    List<EmployeeKitchen> findByKitchenId(UUID kitchenId);
    long countByKitchenId(UUID kitchenId);
    void deleteByEmployeeId(UUID employeeId);
    void deleteByKitchenId(UUID kitchenId);
    boolean existsByEmployeeIdAndKitchenId(UUID employeeId, UUID kitchenId);
}
