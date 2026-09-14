package com.restaurantpos.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DeliveryDashboardDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private long todayOrdersCount;
        private long activeDeliveryCount;
        private long deliveredCount;
        private long cancelledCount;
        private long failedCount;
        private long mappingRequiredCount;

        private BigDecimal grossSales;
        private BigDecimal deliveryFeesTotal;
        private BigDecimal commissionTotal;
        private BigDecimal netDeliveryRevenue;

        @Builder.Default
        private List<ProviderStat> providerStats = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderStat {
        private String providerCode;
        private String providerName;
        private String status;
        private long orderCount;
        private BigDecimal totalSales;
    }
}
