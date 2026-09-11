package com.restaurantpos.inventory.entity;

import com.restaurantpos.tenants.entity.Tenant;
import com.restaurantpos.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "inventory_audits")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditStatus status = AuditStatus.IN_PROGRESS;

    private String notes;

    @Column(name = "total_items_counted", nullable = false)
    private int totalItemsCounted = 0;

    @Column(name = "total_difference_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDifferenceCost = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    private User completedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "audit", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InventoryAuditItem> items = new ArrayList<>();

    public enum AuditStatus {
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
}
