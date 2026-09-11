package com.restaurantpos.reports.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ReportDto {

    // ==========================================
    // 1. SAVDO HISOBOTI (SALES SUMMARY)
    // ==========================================
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SalesSummary {
        private BigDecimal totalSales;
        private long totalOrders;
        private BigDecimal totalPayments;
        private BigDecimal avgCheck;
        private BigDecimal cashTotal;
        private BigDecimal cardTotal;
        private BigDecimal otherTotal;
        private long cancelledOrdersCount;
        private long refundedOrdersCount;
        private BigDecimal refundedAmount;
        private List<HourlySales> hourlySales;
        private List<DailyTrend> dailyTrend;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class HourlySales {
        private int hour;
        private long orders;
        private BigDecimal revenue;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DailyTrend {
        private String date;
        private long orders;
        private BigDecimal revenue;
    }

    // ==========================================
    // 2. MAHSULOT SAVDO HISOBOTI (PRODUCT SALES)
    // ==========================================
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProductSaleItem {
        private UUID productId;
        private String productName;
        private String categoryName;
        private BigDecimal quantity;
        private BigDecimal revenue;
        private BigDecimal cost;
        private BigDecimal profit;
        private BigDecimal profitMargin; // percent
    }

    // ==========================================
    // 3. FOYDA HISOBOTI (PROFIT & LOSS)
    // ==========================================
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProfitLoss {
        private BigDecimal totalRevenue;
        private BigDecimal totalProductCost;
        private BigDecimal totalDiscounts;
        private BigDecimal totalRefunds;
        private BigDecimal grossProfit;
        private BigDecimal profitMargin;
    }

    // ==========================================
    // 4. KASSA HISOBOTI (CASHIER / REGISTER)
    // ==========================================
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CashierSummary {
        private UUID cashierId;
        private String cashierName;
        private long ordersCount;
        private BigDecimal cashSales;
        private BigDecimal cardSales;
        private BigDecimal onlineSales;
        private BigDecimal otherSales;
        private BigDecimal totalSales;
        private BigDecimal totalRefunds;
    }

    // ==========================================
    // 5. OFITSIANT HISOBOTI (WAITER PERFORMANCE)
    // ==========================================
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WaiterPerformance {
        private UUID waiterId;
        private String waiterName;
        private long ordersCount;
        private BigDecimal totalSales;
        private BigDecimal avgCheck;
        private long deliveredCount;
        private long cancelledCount;
    }

    // ==========================================
    // 6. OSHXONA HISOBOTI (KITCHEN PERFORMANCE)
    // ==========================================
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class KitchenPerformance {
        private UUID kitchenId;
        private String kitchenName;
        private long ordersCount;
        private BigDecimal itemsPrepared;
        private BigDecimal itemsCancelled;
        private BigDecimal revenue;
    }

    // ==========================================
    // 7. STOCK REPORT (OMBOR HARAKATI / BALANCE)
    // ==========================================
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StockReportItem {
        private UUID itemId;
        private String itemName;
        private String unit;
        private String category;
        private String warehouseName;
        private BigDecimal openingStock;
        private BigDecimal incoming;
        private BigDecimal outgoing;
        private BigDecimal salesConsumption;
        private BigDecimal waste;
        private BigDecimal adjustment;
        private BigDecimal closingStock;
        private BigDecimal unitCost;
        private BigDecimal totalValuation;
    }

    // Legacy DailySummary / ShiftSummary for backward compatibility
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
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
        private List<HourlySales> hourlyRevenue;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TopProduct {
        private String productName;
        private long quantity;
        private BigDecimal revenue;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ShiftSummary {
        private UUID shiftId;
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
