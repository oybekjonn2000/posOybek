package com.restaurantpos.inventory.entity;

import com.restaurantpos.tenants.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_items")
@Getter @Setter @NoArgsConstructor
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private String name;

    @Column(length = 100)
    private String sku;

    @Column(nullable = false, length = 50)
    private String unit = "pcs";

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(name = "min_quantity", nullable = false, precision = 15, scale = 3)
    private BigDecimal minQuantity = BigDecimal.ZERO;

    @Column(name = "max_quantity", precision = 15, scale = 3)
    private BigDecimal maxQuantity = BigDecimal.ZERO;

    @Column(name = "cost_price", precision = 15, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "selling_price", precision = 15, scale = 2)
    private BigDecimal sellingPrice;

    @Column(length = 100)
    private String category;

    private String notes;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public boolean isLowStock() {
        return quantity.compareTo(minQuantity) <= 0 && quantity.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isOutOfStock() {
        return quantity.compareTo(BigDecimal.ZERO) <= 0;
    }
}
