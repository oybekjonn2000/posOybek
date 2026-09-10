package com.restaurantpos.customers.repository;

import com.restaurantpos.customers.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    List<Customer> findByTenantIdAndDeletedAtIsNullOrderByFullNameAsc(UUID tenantId);

    Optional<Customer> findByTenantIdAndPhoneAndDeletedAtIsNull(UUID tenantId, String phone);

    Optional<Customer> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
}
