package com.restaurantpos.delivery.entity;

import com.restaurantpos.common.entity.BaseEntity;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.tenants.entity.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "delivery_orders", uniqueConstraints = {
        @UniqueConstraint(name = "uq_delivery_orders_provider_external", columnNames = {"provider_id", "external_order_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class DeliveryOrderEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "provider_id", nullable = false)
    private DeliveryProviderEntity provider;

    @Column(name = "external_order_id", nullable = false, length = 100)
    private String externalOrderId;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pos_order_id")
    private Order posOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private DeliveryOrderStatus status = DeliveryOrderStatus.NEW;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(name = "customer_phone", length = 50)
    private String customerPhone;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "address_apartment", length = 50)
    private String addressApartment;

    @Column(name = "address_entrance", length = 50)
    private String addressEntrance;

    @Column(name = "address_floor", length = 50)
    private String addressFloor;

    @Column(name = "address_comment", columnDefinition = "TEXT")
    private String addressComment;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "delivery_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(name = "commission", nullable = false, precision = 15, scale = 2)
    private BigDecimal commission = BigDecimal.ZERO;

    @Column(name = "discount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "service_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal serviceFee = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 15, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "payment_type", nullable = false, length = 50)
    private String paymentType = "ONLINE";

    @Column(name = "payment_status", nullable = false, length = 50)
    private String paymentStatus = "PENDING";

    @Column(name = "courier_name", length = 100)
    private String courierName;

    @Column(name = "courier_phone", length = 50)
    private String courierPhone;

    @Column(name = "courier_id", length = 100)
    private String courierId;

    @Column(name = "courier_vehicle", length = 50)
    private String courierVehicle;

    @Column(name = "courier_status", length = 50)
    private String courierStatus;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @OneToMany(mappedBy = "deliveryOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DeliveryOrderItemEntity> items = new ArrayList<>();

    public enum DeliveryOrderStatus {
        NEW,
        ACCEPTED,
        REJECTED,
        PREPARING,
        READY,
        COURIER_ASSIGNED,
        PICKED_UP,
        DELIVERING,
        DELIVERED,
        CANCELLED,
        FAILED,
        MAPPING_REQUIRED
    }
}
