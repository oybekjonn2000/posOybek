package com.restaurantpos.orders.entity;

import com.restaurantpos.products.entity.Product;
import com.restaurantpos.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order line item entity.
 * Immutable once voided — void creates a record rather than deleting.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kitchen_id")
    private com.restaurantpos.kitchen.entity.Kitchen kitchen;

    // Snapshot of product name at time of order (product may change later)
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "product_sku", length = 100)
    private String productSku;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "notes")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "kitchen_status", length = 20)
    private KitchenStatus kitchenStatus = KitchenStatus.NEW;

    @Column(name = "is_voided", nullable = false)
    private boolean voided = false;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voided_by")
    private User voidedBy;

    @Column(name = "void_reason")
    private String voidReason;

    @Column(name = "sent_quantity", nullable = false, precision = 10, scale = 3)
    private BigDecimal sentQuantity = BigDecimal.ZERO;

    @Column(name = "delivered_quantity", nullable = false, precision = 10, scale = 3)
    private BigDecimal deliveredQuantity = BigDecimal.ZERO;

    @Column(name = "cancelled_quantity", nullable = false, precision = 10, scale = 3)
    private BigDecimal cancelledQuantity = BigDecimal.ZERO;

    @Column(name = "sort_order")
    private int sortOrder = 0;

    @Column(name = "sent_to_kitchen_at")
    private Instant sentToKitchenAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemModifier> modifiers = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Calculates subtotal from unit price, quantity, modifiers, and discount.
     */
    public void calculateSubtotal() {
        BigDecimal modifiersTotal = modifiers.stream()
                .map(m -> m.getPrice().multiply(BigDecimal.valueOf(m.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal itemPrice = unitPrice.add(modifiersTotal);
        BigDecimal gross = itemPrice.multiply(quantity);

        if (discountPercent != null && discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            discountAmount = gross.multiply(discountPercent).divide(BigDecimal.valueOf(100));
        }

        this.subtotal = gross.subtract(discountAmount);
    }

    public BigDecimal getRemainingQuantity() {
        BigDecimal q = quantity != null ? quantity : BigDecimal.ZERO;
        BigDecimal s = sentQuantity != null ? sentQuantity : BigDecimal.ZERO;
        return q.subtract(s).max(BigDecimal.ZERO);
    }

    public enum KitchenStatus {
        NEW,
        PARTIALLY_SENT,
        SENT_TO_KITCHEN,
        ACCEPTED,
        PREPARING,
        COOKING,
        READY,
        DELIVERED,
        SERVED,
        CANCELLED
    }
}
