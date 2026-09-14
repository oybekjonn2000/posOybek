package com.restaurantpos.delivery.repository;

import com.restaurantpos.delivery.entity.DeliveryOrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrderEntity, UUID> {

    Optional<DeliveryOrderEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<DeliveryOrderEntity> findByProviderIdAndExternalOrderId(UUID providerId, String externalOrderId);

    Optional<DeliveryOrderEntity> findByPosOrderId(UUID posOrderId);

    List<DeliveryOrderEntity> findByTenantIdAndCreatedAtBetween(UUID tenantId, Instant start, Instant end);

    @Query("SELECT o FROM DeliveryOrderEntity o WHERE o.tenant.id = :tenantId " +
           "AND (:providerId IS NULL OR o.provider.id = :providerId) " +
           "AND (:status IS NULL OR o.status = :status) " +
           "AND (:paymentType IS NULL OR o.paymentType = :paymentType) " +
           "AND (:search IS NULL OR LOWER(o.externalOrderId) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(o.customerPhone) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY o.createdAt DESC")
    Page<DeliveryOrderEntity> findFiltered(
            @Param("tenantId") UUID tenantId,
            @Param("providerId") UUID providerId,
            @Param("status") DeliveryOrderEntity.DeliveryOrderStatus status,
            @Param("paymentType") String paymentType,
            @Param("search") String search,
            Pageable pageable
    );

    long countByTenantIdAndStatus(UUID tenantId, DeliveryOrderEntity.DeliveryOrderStatus status);

    long countByTenantIdAndCreatedAtBetween(UUID tenantId, Instant start, Instant end);
}
