package com.restaurantpos.delivery.provider.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalDeliveryProduct {
    private String externalProductId;
    private String name;
    private String externalCategoryId;
    private String categoryName;
    private BigDecimal price;
    private String description;
    private boolean available;
}
