package com.restaurantpos.payments.entity;

import com.restaurantpos.devices.entity.Device;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.shifts.entity.Shift;
import com.restaurantpos.tenants.entity.Tenant;
import com.restaurantpos.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payment entity — immutable financial transaction record.
 * NEVER modified after creation; refunds create new payment records.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private Shift shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id")
    private User cashier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(name = "payment_number", nullable = false, length = 50)
    private String paymentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.COMPLETED;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "cash_amount", precision = 15, scale = 2)
    private BigDecimal cashAmount = BigDecimal.ZERO;

    @Column(name = "card_amount", precision = 15, scale = 2)
    private BigDecimal cardAmount = BigDecimal.ZERO;

    @Column(name = "other_amount", precision = 15, scale = 2)
    private BigDecimal otherAmount = BigDecimal.ZERO;

    @Column(name = "change_amount", precision = 15, scale = 2)
    private BigDecimal changeAmount = BigDecimal.ZERO;

    @Column(name = "is_refund", nullable = false)
    private boolean refund = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_payment_id")
    private Payment originalPayment;

    @Column(name = "refund_reason")
    private String refundReason;

    @Column(name = "reference_number", length = 255)
    private String referenceNumber;

    @Column(name = "notes")
    private String notes;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public enum PaymentMethod {
        CASH, CARD, OTHER, MIXED
    }

    public enum PaymentStatus {
        PENDING, COMPLETED, REFUNDED, FAILED
    }
}
