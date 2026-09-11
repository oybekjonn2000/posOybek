package com.restaurantpos.orders.entity;

import com.restaurantpos.common.entity.BaseEntity;
import com.restaurantpos.customers.entity.Customer;
import com.restaurantpos.devices.entity.Device;
import com.restaurantpos.shifts.entity.Shift;
import com.restaurantpos.tables.entity.RestaurantTable;
import com.restaurantpos.tenants.entity.Tenant;
import com.restaurantpos.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Order entity — core business entity of the POS system.
 * Supports DINE_IN, TAKEAWAY, and DELIVERY order types.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private Shift shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private RestaurantTable table;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waiter_id")
    private User waiter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id")
    private User cashier;

    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    private OrderType orderType = OrderType.DINE_IN;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status = OrderStatus.OPEN;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 15, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    // Delivery fields
    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "delivery_phone", length = 50)
    private String deliveryPhone;

    @Column(name = "delivery_notes")
    private String deliveryNotes;

    @Column(name = "delivery_fee", precision = 15, scale = 2)
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(name = "notes")
    private String notes;

    @Column(name = "kitchen_notes")
    private String kitchenNotes;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt = Instant.now();

    @Column(name = "sent_to_kitchen_at")
    private Instant sentToKitchenAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version = 1;

    @Column(name = "local_sequence")
    private Long localSequence;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    /**
     * Recalculates order totals from items.
     * Must be called after adding/removing/modifying items.
     */
    public void recalculateTotals() {
        this.subtotal = items.stream()
                .filter(item -> !item.isVoided())
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Apply discount
        if (discountPercent != null && discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            this.discountAmount = subtotal.multiply(discountPercent).divide(BigDecimal.valueOf(100));
        }

        BigDecimal taxableAmount = subtotal.subtract(discountAmount);
        this.taxAmount = items.stream()
                .filter(item -> !item.isVoided())
                .map(item -> item.getTaxAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.total = taxableAmount.add(taxAmount).add(
                deliveryFee != null ? deliveryFee : BigDecimal.ZERO);
    }

    public enum OrderType {
        DINE_IN, TAKEAWAY, DELIVERY
    }

    public enum OrderStatus {
        OPEN, IN_PROGRESS, READY, PAID, CANCELLED, REFUNDED
    }

    public enum ReceiptPrintStatus {
        NOT_PRINTED, PRINTING, PRINTED, PRINT_FAILED
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "receipt_print_status", nullable = false, length = 30)
    private ReceiptPrintStatus receiptPrintStatus = ReceiptPrintStatus.NOT_PRINTED;

    @Column(name = "receipt_printed_at")
    private Instant receiptPrintedAt;

    @Column(name = "receipt_print_attempts", nullable = false)
    private int receiptPrintAttempts = 0;

    @Column(name = "receipt_print_error")
    private String receiptPrintError;
}
