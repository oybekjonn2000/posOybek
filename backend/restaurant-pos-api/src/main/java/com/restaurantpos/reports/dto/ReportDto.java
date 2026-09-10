package com.restaurantpos.reports.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class ReportDto {

    @Data @Builder
    public static class DailySummary {
        private Instant date;
        private BigDecimal totalRevenue;
        private BigDecimal cashRevenue;
        private BigDecimal cardRevenue;
        private BigDecimal totalRefunds;
        private long totalOrders;
        private long completedOrders;
        private long refundedOrders;
        private BigDecimal avgOrderValue;
        private List<TopProduct> topProducts;
        private List<HourlyRevenue> hourlyRevenue;
    }

    @Data @Builder
    public static class TopProduct {
        private String productName;
        private long quantity;
        private BigDecimal revenue;
    }

    @Data @Builder
    public static class HourlyRevenue {
        private int hour;
        private long orders;
        private BigDecimal revenue;
    }

    @Data @Builder
    public static class ShiftSummary {
        private java.util.UUID shiftId;
        private String cashierName;
        private Instant openedAt;
        private Instant closedAt;
        private BigDecimal totalSales;
        private BigDecimal totalCash;
        private BigDecimal totalCard;
        private BigDecimal totalRefunds;
        private long ordersCount;
    }
}
