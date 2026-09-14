package com.restaurantpos.delivery.entity;

import com.restaurantpos.common.entity.BaseEntity;
import com.restaurantpos.products.entity.Product;
import com.restaurantpos.tenants.entity.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "delivery_product_mappings", uniqueConstraints = {
        @UniqueConstraint(name = "uq_delivery_product_mappings", columnNames = {"provider_id", "external_product_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class DeliveryProductMappingEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private DeliveryProviderEntity provider;

    @Column(name = "external_product_id", nullable = false, length = 100)
    private String externalProductId;

    @Column(name = "external_product_name", length = 255)
    private String externalProductName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pos_product_id", nullable = false)
    private Product posProduct;

    @Column(name = "auto_mapped", nullable = false)
    private boolean autoMapped = false;
}
