package com.restaurantpos.payments.repository;

import com.restaurantpos.payments.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByOrderId(UUID orderId);

    List<Payment> findByTenantIdOrderByPaidAtDesc(UUID tenantId);

    List<Payment> findByShiftId(UUID shiftId);

    Optional<Payment> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Payment> findByTenantIdAndPaidAtBetween(UUID tenantId, Instant from, Instant to);

    // Reports uchun
    List<Payment> findByTenantIdAndPaidAtBetweenAndRefundFalse(UUID tenantId, Instant from, Instant to);

    List<Payment> findByTenantIdAndPaidAtBetweenAndRefundTrue(UUID tenantId, Instant from, Instant to);
}
