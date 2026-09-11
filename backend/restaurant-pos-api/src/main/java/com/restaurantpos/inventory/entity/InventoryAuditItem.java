package com.restaurantpos.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_audit_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryAuditItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_id", nullable = false)
    private InventoryAudit audit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Column(name = "system_quantity", nullable = false, precision = 15, scale = 3)
    private BigDecimal systemQuantity;

    @Column(name = "actual_quantity", nullable = false, precision = 15, scale = 3)
    private BigDecimal actualQuantity;

    @Column(name = "difference_quantity", nullable = false, precision = 15, scale = 3)
    private BigDecimal differenceQuantity;

    @Column(name = "cost_price", precision = 15, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "difference_cost", precision = 15, scale = 2)
    private BigDecimal differenceCost = BigDecimal.ZERO;

    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
