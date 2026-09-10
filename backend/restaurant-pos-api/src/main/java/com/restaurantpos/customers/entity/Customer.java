package com.restaurantpos.customers.entity;

import com.restaurantpos.common.entity.BaseEntity;
import com.restaurantpos.tenants.entity.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Customer entity for CRM and delivery.
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
public class Customer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "address")
    private String address;

    @Column(name = "notes")
    private String notes;

    @Column(name = "total_orders")
    private int totalOrders = 0;

    @Column(name = "total_spent", precision = 15, scale = 2)
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(name = "last_order_at")
    private Instant lastOrderAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
