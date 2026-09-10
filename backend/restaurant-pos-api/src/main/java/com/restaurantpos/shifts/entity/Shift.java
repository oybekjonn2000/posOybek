package com.restaurantpos.shifts.entity;

import com.restaurantpos.devices.entity.Device;
import com.restaurantpos.tenants.entity.Tenant;
import com.restaurantpos.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Shift entity — cashier work session tracking cash movements and sales totals.
 */
@Entity
@Table(name = "shifts")
@Getter
@Setter
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;

    @Column(name = "shift_number", nullable = false, length = 50)
    private String shiftNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ShiftStatus status = ShiftStatus.OPEN;

    @Column(name = "opening_cash", nullable = false, precision = 15, scale = 2)
    private BigDecimal openingCash = BigDecimal.ZERO;

    @Column(name = "closing_cash_expected", precision = 15, scale = 2)
    private BigDecimal closingCashExpected = BigDecimal.ZERO;

    @Column(name = "closing_cash_actual", precision = 15, scale = 2)
    private BigDecimal closingCashActual;

    @Column(name = "cash_difference", precision = 15, scale = 2)
    private BigDecimal cashDifference;

    @Column(name = "total_sales", precision = 15, scale = 2)
    private BigDecimal totalSales = BigDecimal.ZERO;

    @Column(name = "total_cash_sales", precision = 15, scale = 2)
    private BigDecimal totalCashSales = BigDecimal.ZERO;

    @Column(name = "total_card_sales", precision = 15, scale = 2)
    private BigDecimal totalCardSales = BigDecimal.ZERO;

    @Column(name = "total_refunds", precision = 15, scale = 2)
    private BigDecimal totalRefunds = BigDecimal.ZERO;

    @Column(name = "total_discounts", precision = 15, scale = 2)
    private BigDecimal totalDiscounts = BigDecimal.ZERO;

    @Column(name = "orders_count")
    private int ordersCount = 0;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt = Instant.now();

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public enum ShiftStatus {
        OPEN, CLOSED
    }
}
