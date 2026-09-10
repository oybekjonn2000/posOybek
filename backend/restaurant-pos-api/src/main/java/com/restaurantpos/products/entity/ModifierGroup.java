package com.restaurantpos.products.entity;

import com.restaurantpos.tenants.entity.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Modifier group — a group of options that can be applied to a product.
 * Example: "Size" (Small/Medium/Large), "Extras" (Cheese/Bacon).
 */
@Entity
@Table(name = "modifier_groups")
@Getter
@Setter
public class ModifierGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "is_required", nullable = false)
    private boolean required = false;

    @Column(name = "min_selections")
    private int minSelections = 0;

    @Column(name = "max_selections")
    private int maxSelections = 1;

    @Column(name = "sort_order")
    private int sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Modifier> modifiers = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
