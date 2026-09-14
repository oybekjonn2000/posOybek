package com.restaurantpos.delivery.entity;

import com.restaurantpos.tenants.entity.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_webhook_events", uniqueConstraints = {
        @UniqueConstraint(name = "uq_delivery_webhook_events", columnNames = {"provider_id", "external_event_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class DeliveryWebhookEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private DeliveryProviderEntity provider;

    @Column(name = "external_event_id", nullable = false, length = 100)
    private String externalEventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "processed", nullable = false)
    private boolean processed = false;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;
}
