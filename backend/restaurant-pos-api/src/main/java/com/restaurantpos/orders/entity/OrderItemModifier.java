package com.restaurantpos.orders.entity;

import com.restaurantpos.products.entity.Modifier;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Applied modifier on an order item.
 */
@Entity
@Table(name = "order_item_modifiers")
@Getter
@Setter
public class OrderItemModifier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modifier_id", nullable = false)
    private Modifier modifier;

    // Snapshot of modifier name and price at order time
    @Column(name = "modifier_name", nullable = false, length = 255)
    private String modifierName;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "quantity", nullable = false)
    private int quantity = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
