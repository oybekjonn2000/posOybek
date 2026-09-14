package com.restaurantpos.delivery.repository;

import com.restaurantpos.delivery.entity.DeliveryWebhookEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryWebhookEventRepository extends JpaRepository<DeliveryWebhookEventEntity, UUID> {
    Optional<DeliveryWebhookEventEntity> findByProviderIdAndExternalEventId(UUID providerId, String externalEventId);
    boolean existsByProviderIdAndExternalEventId(UUID providerId, String externalEventId);
}
