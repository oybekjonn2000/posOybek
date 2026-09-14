package com.restaurantpos.delivery.entity;

import com.restaurantpos.products.entity.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "delivery_order_items")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryOrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_order_id", nullable = false)
    private DeliveryOrderEntity deliveryOrder;

    @Column(name = "external_product_id", length = 100)
    private String externalProductId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pos_product_id")
    private Product posProduct;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 15, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "mapping_status", nullable = false, length = 30)
    private String mappingStatus = "MAPPED"; // MAPPED, UNMAPPED

    @Column(name = "notes")
    private String notes;
}
