package com.restaurantpos.delivery.provider.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalDeliveryOrder {
    private String externalOrderId;
    private String providerCode;
    private String status; // NEW, ACCEPTED, PREPARING, READY, PICKED_UP, DELIVERED, CANCELLED
    private String customerName;
    private String customerPhone;
    private String deliveryAddress;
    private String addressApartment;
    private String addressEntrance;
    private String addressFloor;
    private String addressComment;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal commission;
    private BigDecimal discount;
    private BigDecimal serviceFee;
    private BigDecimal total;
    private String paymentType; // ONLINE, CASH, CARD, UNKNOWN
    private String paymentStatus; // PAID, PENDING, FAILED
    private DeliveryCourierInfo courier;
    private Instant createdAt;
    private String rawPayload;

    @Builder.Default
    private List<ExternalDeliveryOrderItem> items = new ArrayList<>();
}
