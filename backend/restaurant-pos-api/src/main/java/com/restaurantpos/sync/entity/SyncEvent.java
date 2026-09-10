package com.restaurantpos.sync.entity;

import com.restaurantpos.devices.entity.Device;
import com.restaurantpos.tenants.entity.Tenant;
import com.restaurantpos.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Sync event — immutable record of a local change that needs to be synced to cloud.
 * This is the heart of the offline-first synchronization engine.
 */
@Entity
@Table(name = "sync_events")
@Getter
@Setter
public class SyncEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 20)
    private Operation operation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "version", nullable = false)
    private long version = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SyncStatus status = SyncStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "synced_at")
    private Instant syncedAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    public enum Operation {
        CREATE, UPDATE, DELETE
    }

    public enum SyncStatus {
        PENDING, SYNCING, SYNCED, FAILED
    }

    /**
     * Calculates next retry time using exponential backoff.
     * Retry delays: 1min, 5min, 15min, 30min, 1hr
     */
    public void scheduleRetry() {
        this.retryCount++;
        this.status = SyncStatus.FAILED;
        long[] delays = {60, 300, 900, 1800, 3600};
        int index = Math.min(retryCount - 1, delays.length - 1);
        this.nextRetryAt = Instant.now().plusSeconds(delays[index]);
    }
}
