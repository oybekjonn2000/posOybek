package com.restaurantpos.products.entity;

import com.restaurantpos.common.entity.BaseEntity;
import com.restaurantpos.tenants.entity.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Product entity — menu items that can be ordered.
 * All prices stored as NUMERIC(15,2) via BigDecimal.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
public class Product extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kitchen_id")
    private com.restaurantpos.kitchen.entity.Kitchen kitchen;

    @Column(name = "sku", length = 100)
    private String sku;

    @Column(name = "barcode", length = 100)
    private String barcode;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "name_uz", length = 255)
    private String nameUz;

    @Column(name = "name_ru", length = 255)
    private String nameRu;

    @Column(name = "name_en", length = 255)
    private String nameEn;

    @Column(name = "description")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "unit", nullable = false, length = 50)
    private String unit = "piece";

    @Column(name = "purchase_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Column(name = "sale_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal salePrice = BigDecimal.ZERO;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "is_taxable", nullable = false)
    private boolean taxable = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_available", nullable = false)
    private boolean available = true;

    @Column(name = "track_stock", nullable = false)
    private boolean trackStock = true;

    @Column(name = "min_stock_level", precision = 15, scale = 3)
    private BigDecimal minStockLevel = BigDecimal.ZERO;

    @Column(name = "current_stock", precision = 15, scale = 3)
    private BigDecimal currentStock = BigDecimal.ZERO;

    @Column(name = "sort_order")
    private int sortOrder = 0;

    @Version
    @Column(name = "version", nullable = false)
    private int version = 1;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_modifier_groups",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "modifier_group_id")
    )
    private Set<ModifierGroup> modifierGroups = new HashSet<>();

    public boolean isLowStock() {
        return trackStock && currentStock.compareTo(minStockLevel) <= 0;
    }

    public com.restaurantpos.kitchen.entity.Kitchen getKitchen() {
        if (category != null && category.getKitchen() != null) {
            return category.getKitchen();
        }
        return this.kitchen;
    }

    public void setCategory(Category category) {
        this.category = category;
        if (category != null && category.getKitchen() != null) {
            this.kitchen = category.getKitchen();
        }
    }
}
