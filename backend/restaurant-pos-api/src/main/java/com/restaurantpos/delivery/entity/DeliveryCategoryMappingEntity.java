package com.restaurantpos.delivery.entity;

import com.restaurantpos.common.entity.BaseEntity;
import com.restaurantpos.products.entity.Category;
import com.restaurantpos.tenants.entity.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "delivery_category_mappings", uniqueConstraints = {
        @UniqueConstraint(name = "uq_delivery_category_mappings", columnNames = {"provider_id", "external_category_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class DeliveryCategoryMappingEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private DeliveryProviderEntity provider;

    @Column(name = "external_category_id", nullable = false, length = 100)
    private String externalCategoryId;

    @Column(name = "external_category_name", length = 255)
    private String externalCategoryName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pos_category_id", nullable = false)
    private Category posCategory;
}
