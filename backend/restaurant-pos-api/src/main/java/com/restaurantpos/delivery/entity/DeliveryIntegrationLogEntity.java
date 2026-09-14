package com.restaurantpos.delivery.entity;

import com.restaurantpos.tenants.entity.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_integration_logs")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryIntegrationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "provider_id")
    private DeliveryProviderEntity provider;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private LogStatus status = LogStatus.SUCCESS;

    @Column(name = "request_time_ms")
    private Long requestTimeMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public enum LogStatus {
        SUCCESS, FAILED, PENDING, WARNING
    }
}
