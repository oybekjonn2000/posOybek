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
public class ExternalDeliveryOrderItem {
    private String externalProductId;
    private String name;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal total;
    private String notes;
}
