package com.restaurantpos.delivery.entity;

import com.restaurantpos.common.entity.BaseEntity;
import com.restaurantpos.tenants.entity.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "delivery_providers", uniqueConstraints = {
        @UniqueConstraint(name = "uq_delivery_providers_tenant_code", columnNames = {"tenant_id", "code"})
})
@Getter
@Setter
@NoArgsConstructor
public class DeliveryProviderEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 50)
    private ProviderType providerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProviderStatus status = ProviderStatus.DISCONNECTED;

    @Column(name = "api_base_url")
    private String apiBaseUrl;

    @Column(name = "encrypted_api_key", columnDefinition = "TEXT")
    private String encryptedApiKey;

    @Column(name = "encrypted_client_id", columnDefinition = "TEXT")
    private String encryptedClientId;

    @Column(name = "encrypted_secret", columnDefinition = "TEXT")
    private String encryptedSecret;

    @Column(name = "restaurant_id", length = 100)
    private String restaurantId;

    @Column(name = "webhook_url")
    private String webhookUrl;

    @Column(name = "webhook_secret", columnDefinition = "TEXT")
    private String webhookSecret;

    @Column(name = "auto_accept", nullable = false)
    private boolean autoAccept = false;

    @Column(name = "auto_print_kitchen", nullable = false)
    private boolean autoPrintKitchen = true;

    @Column(name = "auto_print_receipt", nullable = false)
    private boolean autoPrintReceipt = false;

    @Column(name = "sound_notification", nullable = false)
    private boolean soundNotification = true;

    @Column(name = "auto_sync", nullable = false)
    private boolean autoSync = true;

    @Column(name = "sync_interval_seconds", nullable = false)
    private int syncIntervalSeconds = 60;

    @Enumerated(EnumType.STRING)
    @Column(name = "commission_type", nullable = false, length = 30)
    private CommissionType commissionType = CommissionType.PERCENTAGE;

    @Column(name = "commission_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal commissionValue = BigDecimal.ZERO;

    @Column(name = "default_order_source", nullable = false, length = 50)
    private String defaultOrderSource = "DELIVERY";

    @Column(name = "default_payment_type", nullable = false, length = 50)
    private String defaultPaymentType = "ONLINE";

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Column(name = "last_connection_test_at")
    private Instant lastConnectionTestAt;

    @Column(name = "last_connection_status", length = 30)
    private String lastConnectionStatus;

    @Column(name = "last_connection_error", columnDefinition = "TEXT")
    private String lastConnectionError;

    public enum ProviderType {
        YANDEX, UZUM, GLOVO, CUSTOM
    }

    public enum ProviderStatus {
        CONNECTED, DISCONNECTED, ERROR, DISABLED
    }

    public enum CommissionType {
        PERCENTAGE, FIXED, PROVIDER_REPORTED
    }
}
