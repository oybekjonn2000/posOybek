package com.restaurantpos.devices.entity;

import com.restaurantpos.tenants.entity.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Device entity — represents a POS terminal, kitchen display, or manager device.
 */
@Entity
@Table(name = "devices")
@Getter
@Setter
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "device_code", nullable = false, length = 50)
    private String deviceCode;

    @Column(name = "device_name", nullable = false, length = 255)
    private String deviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 50)
    private DeviceType deviceType;

    @Column(name = "token", length = 500)
    private String token;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "mac_address", length = 20)
    private String macAddress;

    @Column(name = "os_info", length = 255)
    private String osInfo;

    @Column(name = "app_version", length = 50)
    private String appVersion;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_master", nullable = false)
    private boolean master = false;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum DeviceType {
        POS, KITCHEN, MANAGER, SERVER, WAITER
    }
}
