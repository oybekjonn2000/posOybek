package com.restaurantpos.reports.service;

import com.restaurantpos.auth.security.UserPrincipal;
import com.restaurantpos.inventory.entity.InventoryItem;
import com.restaurantpos.inventory.entity.InventoryTransaction;
import com.restaurantpos.inventory.entity.ProductIngredient;
import com.restaurantpos.inventory.repository.InventoryItemRepository;
import com.restaurantpos.inventory.repository.InventoryTransactionRepository;
import com.restaurantpos.inventory.repository.ProductIngredientRepository;
import com.restaurantpos.kitchen.entity.Kitchen;
import com.restaurantpos.kitchen.repository.KitchenRepository;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.entity.OrderItem;
import com.restaurantpos.orders.repository.OrderRepository;
import com.restaurantpos.payments.entity.Payment;
import com.restaurantpos.payments.repository.PaymentRepository;
import com.restaurantpos.products.entity.Product;
import com.restaurantpos.products.repository.ProductRepository;
import com.restaurantpos.reports.dto.ReportDto;
import com.restaurantpos.shifts.entity.Shift;
import com.restaurantpos.shifts.repository.ShiftRepository;
import com.restaurantpos.users.entity.User;
import com.restaurantpos.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportsService {

    private final PaymentRepository paymentRepository;
    private final ShiftRepository shiftRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductIngredientRepository productIngredientRepository;
    private final KitchenRepository kitchenRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryTransactionRepository inventoryTxRepository;
    private final UserRepository userRepository;

    private static final ZoneId TASHKENT_ZONE = ZoneId.of("Asia/Tashkent");

    // ==========================================
    // 1. SAVDO HISOBOTI (SALES SUMMARY)
    // ==========================================
    @Transactional(readOnly = true)
    public ReportDto.SalesSummary getSalesSummary(UUID tenantId, Instant from, Instant to, UUID waiterId, UUID kitchenId) {
        List<Payment> payments = paymentRepository.findByTenantIdAndPaidAtBetween(tenantId, from, to);

        if (waiterId != null) {
            payments = payments.stream()
                    .filter(p -> p.getOrder() != null && p.getOrder().getWaiter() != null && waiterId.equals(p.getOrder().getWaiter().getId()))
                    .collect(Collectors.toList());
        }

        BigDecimal cashTotal = BigDecimal.ZERO;
        BigDecimal cardTotal = BigDecimal.ZERO;
        BigDecimal otherTotal = BigDecimal.ZERO;
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal refundedAmount = BigDecimal.ZERO;
        long refundedOrdersCount = 0;
        Set<UUID> paidOrderIds = new HashSet<>();

        for (Payment p : payments) {
            if (p.isRefund()) {
                refundedAmount = refundedAmount.add(p.getAmount().abs());
                refundedOrdersCount++;
            } else {
                BigDecimal cash = p.getCashAmount() != null ? p.getCashAmount() : BigDecimal.ZERO;
                BigDecimal card = p.getCardAmount() != null ? p.getCardAmount() : BigDecimal.ZERO;
                BigDecimal amt = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
                BigDecimal other = amt.subtract(cash).subtract(card).max(BigDecimal.ZERO);

                cashTotal = cashTotal.add(cash);
                cardTotal = cardTotal.add(card);
                otherTotal = otherTotal.add(other);
                totalSales = totalSales.add(amt);

                if (p.getOrder() != null) {
                    paidOrderIds.add(p.getOrder().getId());
                }
            }
        }

        long totalOrders = paidOrderIds.size();
        BigDecimal totalPayments = totalSales.subtract(refundedAmount);
        BigDecimal avgCheck = totalOrders > 0
                ? totalSales.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long cancelledOrdersCount = orderRepository.countByTenantIdAndStatusAndClosedAtBetweenAndDeletedAtIsNull(
                tenantId, Order.OrderStatus.CANCELLED, from, to);

        // Hourly breakdown (0..23)
        Map<Integer, BigDecimal> hourlyRevMap = new HashMap<>();
        Map<Integer, Long> hourlyOrderCount = new HashMap<>();
        for (int h = 0; h < 24; h++) {
            hourlyRevMap.put(h, BigDecimal.ZERO);
            hourlyOrderCount.put(h, 0L);
        }

        // Daily trend
        Map<String, BigDecimal> dailyRevMap = new TreeMap<>();
        Map<String, Long> dailyOrderCount = new TreeMap<>();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(TASHKENT_ZONE);

        for (Payment p : payments) {
            if (!p.isRefund() && p.getPaidAt() != null) {
                int hour = p.getPaidAt().atZone(TASHKENT_ZONE).getHour();
                hourlyRevMap.put(hour, hourlyRevMap.get(hour).add(p.getAmount()));
                hourlyOrderCount.put(hour, hourlyOrderCount.get(hour) + 1);

                String dayKey = df.format(p.getPaidAt());
                dailyRevMap.put(dayKey, dailyRevMap.getOrDefault(dayKey, BigDecimal.ZERO).add(p.getAmount()));
                dailyOrderCount.put(dayKey, dailyOrderCount.getOrDefault(dayKey, 0L) + 1);
            }
        }

        List<ReportDto.HourlySales> hourlySales = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            hourlySales.add(ReportDto.HourlySales.builder()
                    .hour(h)
                    .orders(hourlyOrderCount.get(h))
                    .revenue(hourlyRevMap.get(h))
                    .build());
        }

        List<ReportDto.DailyTrend> dailyTrend = dailyRevMap.entrySet().stream()
                .map(e -> ReportDto.DailyTrend.builder()
                        .date(e.getKey())
                        .revenue(e.getValue())
                        .orders(dailyOrderCount.getOrDefault(e.getKey(), 0L))
                        .build())
                .collect(Collectors.toList());

        return ReportDto.SalesSummary.builder()
                .totalSales(totalSales)
                .totalOrders(totalOrders)
                .totalPayments(totalPayments)
                .avgCheck(avgCheck)
                .cashTotal(cashTotal)
                .cardTotal(cardTotal)
                .otherTotal(otherTotal)
                .cancelledOrdersCount(cancelledOrdersCount)
                .refundedOrdersCount(refundedOrdersCount)
                .refundedAmount(refundedAmount)
                .hourlySales(hourlySales)
                .dailyTrend(dailyTrend)
                .build();
    }

    // ==========================================
    // 2. MAHSULOT SAVDO HISOBOTI (PRODUCT SALES)
    // ==========================================
    @Transactional(readOnly = true)
    public List<ReportDto.ProductSaleItem> getProductSalesReport(UUID tenantId, Instant from, Instant to, UUID categoryId, UUID kitchenId) {
        List<Order> paidOrders = orderRepository.findByTenantIdAndStatusAndPaidAtBetweenAndDeletedAtIsNull(
                tenantId, Order.OrderStatus.PAID, from, to);

        // Preload recipes for cost calculation
        Map<UUID, BigDecimal> productCostMap = new HashMap<>();
        List<Product> products = productRepository.findByTenantIdAndDeletedAtIsNullOrderBySortOrderAscNameAsc(tenantId);
        for (Product p : products) {
            List<ProductIngredient> ingredients = productIngredientRepository.findByProductId(p.getId());
            BigDecimal unitCost = BigDecimal.ZERO;
            for (ProductIngredient ing : ingredients) {
                if (ing.getInventoryItem() != null && ing.getInventoryItem().getCostPrice() != null) {
                    unitCost = unitCost.add(ing.getQuantity().multiply(ing.getInventoryItem().getCostPrice()));
                }
            }
            if (unitCost.compareTo(BigDecimal.ZERO) == 0 && p.getPurchasePrice() != null) {
                unitCost = p.getPurchasePrice();
            }
            productCostMap.put(p.getId(), unitCost);
        }

        Map<UUID, ProductAgg> aggMap = new HashMap<>();

        for (Order ord : paidOrders) {
            if (ord.getItems() == null) continue;
            for (OrderItem item : ord.getItems()) {
                if (item.isVoided()) continue;
                Product prod = item.getProduct();
                if (prod == null) continue;

                if (categoryId != null && (prod.getCategory() == null || !categoryId.equals(prod.getCategory().getId()))) {
                    continue;
                }
                if (kitchenId != null && (item.getKitchen() == null || !kitchenId.equals(item.getKitchen().getId()))
                        && (prod.getKitchen() == null || !kitchenId.equals(prod.getKitchen().getId()))) {
                    continue;
                }

                BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                if (item.getCancelledQuantity() != null) {
                    qty = qty.subtract(item.getCancelledQuantity()).max(BigDecimal.ZERO);
                }
                if (qty.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal subtotal = item.getSubtotal() != null ? item.getSubtotal() :
                        (item.getUnitPrice() != null ? item.getUnitPrice().multiply(qty) : BigDecimal.ZERO);

                ProductAgg agg = aggMap.computeIfAbsent(prod.getId(), k -> new ProductAgg(
                        prod.getId(),
                        item.getProductName() != null ? item.getProductName() : prod.getName(),
                        prod.getCategory() != null ? prod.getCategory().getName() : "Boshqa",
                        productCostMap.getOrDefault(prod.getId(), BigDecimal.ZERO)
                ));

                agg.totalQuantity = agg.totalQuantity.add(qty);
                agg.totalRevenue = agg.totalRevenue.add(subtotal);
            }
        }

        List<ReportDto.ProductSaleItem> result = new ArrayList<>();
        for (ProductAgg agg : aggMap.values()) {
            BigDecimal totalCost = agg.unitCost.multiply(agg.totalQuantity);
            BigDecimal profit = agg.totalRevenue.subtract(totalCost);
            BigDecimal margin = agg.totalRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? profit.divide(agg.totalRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            result.add(ReportDto.ProductSaleItem.builder()
                    .productId(agg.productId)
                    .productName(agg.productName)
                    .categoryName(agg.categoryName)
                    .quantity(agg.totalQuantity)
                    .revenue(agg.totalRevenue)
                    .cost(totalCost)
                    .profit(profit)
                    .profitMargin(margin)
                    .build());
        }

        result.sort((a, b) -> b.getQuantity().compareTo(a.getQuantity()));
        return result;
    }

    private static class ProductAgg {
        UUID productId;
        String productName;
        String categoryName;
        BigDecimal unitCost;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        ProductAgg(UUID productId, String productName, String categoryName, BigDecimal unitCost) {
            this.productId = productId;
            this.productName = productName;
            this.categoryName = categoryName;
            this.unitCost = unitCost != null ? unitCost : BigDecimal.ZERO;
        }
    }

    // ==========================================
    // 3. FOYDA HISOBOTI (PROFIT & LOSS)
    // ==========================================
    @Transactional(readOnly = true)
    public ReportDto.ProfitLoss getProfitLossReport(UUID tenantId, Instant from, Instant to) {
        List<ReportDto.ProductSaleItem> productSales = getProductSalesReport(tenantId, from, to, null, null);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalProductCost = BigDecimal.ZERO;

        for (ReportDto.ProductSaleItem p : productSales) {
            totalRevenue = totalRevenue.add(p.getRevenue());
            totalProductCost = totalProductCost.add(p.getCost());
        }

        List<Order> paidOrders = orderRepository.findByTenantIdAndStatusAndPaidAtBetweenAndDeletedAtIsNull(
                tenantId, Order.OrderStatus.PAID, from, to);

        BigDecimal totalDiscounts = paidOrders.stream()
                .map(o -> o.getDiscountAmount() != null ? o.getDiscountAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Payment> refunds = paymentRepository.findByTenantIdAndPaidAtBetweenAndRefundTrue(tenantId, from, to);
        BigDecimal totalRefunds = refunds.stream()
                .map(p -> p.getAmount() != null ? p.getAmount().abs() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Gross Profit = Revenue - Product Cost - Discount - Refund
        BigDecimal grossProfit = totalRevenue.subtract(totalProductCost).subtract(totalDiscounts).subtract(totalRefunds);
        BigDecimal profitMargin = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                ? grossProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        return ReportDto.ProfitLoss.builder()
                .totalRevenue(totalRevenue)
                .totalProductCost(totalProductCost)
                .totalDiscounts(totalDiscounts)
                .totalRefunds(totalRefunds)
                .grossProfit(grossProfit)
                .profitMargin(profitMargin)
                .build();
    }

    // ==========================================
    // 4. KASSA HISOBOTI (CASHIER SUMMARY)
    // ==========================================
    @Transactional(readOnly = true)
    public List<ReportDto.CashierSummary> getCashierReport(UUID tenantId, Instant from, Instant to) {
        List<Payment> payments = paymentRepository.findByTenantIdAndPaidAtBetween(tenantId, from, to);

        Map<UUID, CashierAgg> map = new HashMap<>();

        for (Payment p : payments) {
            User c = p.getCashier();
            UUID cId = c != null ? c.getId() : UUID.fromString("00000000-0000-0000-0000-000000000000");
            String cName = c != null ? c.getFirstName() + " " + (c.getLastName() != null ? c.getLastName() : "") : "Noma'lum kassa";

            CashierAgg agg = map.computeIfAbsent(cId, k -> new CashierAgg(cId, cName));

            if (p.isRefund()) {
                agg.totalRefunds = agg.totalRefunds.add(p.getAmount().abs());
            } else {
                BigDecimal cash = p.getCashAmount() != null ? p.getCashAmount() : BigDecimal.ZERO;
                BigDecimal card = p.getCardAmount() != null ? p.getCardAmount() : BigDecimal.ZERO;
                BigDecimal total = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
                BigDecimal other = total.subtract(cash).subtract(card).max(BigDecimal.ZERO);

                agg.cashSales = agg.cashSales.add(cash);
                agg.cardSales = agg.cardSales.add(card);
                agg.otherSales = agg.otherSales.add(other);
                agg.totalSales = agg.totalSales.add(total);
                agg.ordersCount++;
            }
        }

        return map.values().stream().map(a -> ReportDto.CashierSummary.builder()
                .cashierId(a.cashierId)
                .cashierName(a.cashierName)
                .ordersCount(a.ordersCount)
                .cashSales(a.cashSales)
                .cardSales(a.cardSales)
                .onlineSales(BigDecimal.ZERO)
                .otherSales(a.otherSales)
                .totalSales(a.totalSales)
                .totalRefunds(a.totalRefunds)
                .build()).collect(Collectors.toList());
    }

    private static class CashierAgg {
        UUID cashierId;
        String cashierName;
        long ordersCount = 0;
        BigDecimal cashSales = BigDecimal.ZERO;
        BigDecimal cardSales = BigDecimal.ZERO;
        BigDecimal otherSales = BigDecimal.ZERO;
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalRefunds = BigDecimal.ZERO;

        CashierAgg(UUID cashierId, String cashierName) {
            this.cashierId = cashierId;
            this.cashierName = cashierName;
        }
    }

    // ==========================================
    // 5. OFITSIANT HISOBOTI (WAITER PERFORMANCE)
    // ==========================================
    @Transactional(readOnly = true)
    public List<ReportDto.WaiterPerformance> getWaiterReport(UUID tenantId, Instant from, Instant to, UUID filterWaiterId, UserPrincipal principal) {
        // Waiter isolation: Ofitsiant faqat o'z hisobotini ko'ra oladi
        UUID targetWaiterId = filterWaiterId;
        if (principal != null && principal.isWaiter()) {
            targetWaiterId = principal.getUserId();
        }

        List<Order> paidOrders = targetWaiterId != null
                ? orderRepository.findByTenantIdAndStatusAndPaidAtBetweenAndWaiterIdAndDeletedAtIsNull(tenantId, Order.OrderStatus.PAID, from, to, targetWaiterId)
                : orderRepository.findByTenantIdAndStatusAndPaidAtBetweenAndDeletedAtIsNull(tenantId, Order.OrderStatus.PAID, from, to);

        Map<UUID, WaiterAgg> map = new HashMap<>();

        for (Order o : paidOrders) {
            User w = o.getWaiter();
            UUID wId = w != null ? w.getId() : UUID.fromString("00000000-0000-0000-0000-000000000000");
            String wName = w != null ? w.getFirstName() + " " + (w.getLastName() != null ? w.getLastName() : "") : "Noma'lum ofitsiant";

            WaiterAgg agg = map.computeIfAbsent(wId, k -> new WaiterAgg(wId, wName));
            agg.ordersCount++;
            agg.totalSales = agg.totalSales.add(o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO);

            if (o.getItems() != null) {
                for (OrderItem item : o.getItems()) {
                    if (item.isVoided()) {
                        agg.cancelledCount++;
                    } else if (item.getKitchenStatus() == OrderItem.KitchenStatus.DELIVERED || item.getKitchenStatus() == OrderItem.KitchenStatus.SERVED) {
                        agg.deliveredCount++;
                    }
                }
            }
        }

        return map.values().stream().map(a -> {
            BigDecimal avg = a.ordersCount > 0
                    ? a.totalSales.divide(BigDecimal.valueOf(a.ordersCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            return ReportDto.WaiterPerformance.builder()
                    .waiterId(a.waiterId)
                    .waiterName(a.waiterName)
                    .ordersCount(a.ordersCount)
                    .totalSales(a.totalSales)
                    .avgCheck(avg)
                    .deliveredCount(a.deliveredCount)
                    .cancelledCount(a.cancelledCount)
                    .build();
        }).collect(Collectors.toList());
    }

    private static class WaiterAgg {
        UUID waiterId;
        String waiterName;
        long ordersCount = 0;
        BigDecimal totalSales = BigDecimal.ZERO;
        long deliveredCount = 0;
        long cancelledCount = 0;

        WaiterAgg(UUID waiterId, String waiterName) {
            this.waiterId = waiterId;
            this.waiterName = waiterName;
        }
    }

    // ==========================================
    // 6. OSHXONA HISOBOTI (KITCHEN PERFORMANCE)
    // ==========================================
    @Transactional(readOnly = true)
    public List<ReportDto.KitchenPerformance> getKitchenReport(UUID tenantId, Instant from, Instant to, UUID filterKitchenId) {
        List<Kitchen> kitchens = kitchenRepository.findByTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(tenantId);
        if (filterKitchenId != null) {
            kitchens = kitchens.stream().filter(k -> filterKitchenId.equals(k.getId())).collect(Collectors.toList());
        }

        List<Order> paidOrders = orderRepository.findByTenantIdAndStatusAndPaidAtBetweenAndDeletedAtIsNull(
                tenantId, Order.OrderStatus.PAID, from, to);

        Map<UUID, KitchenAgg> map = new HashMap<>();
        for (Kitchen k : kitchens) {
            map.put(k.getId(), new KitchenAgg(k.getId(), k.getName()));
        }

        for (Order o : paidOrders) {
            if (o.getItems() == null) continue;
            Set<UUID> touchedKitchens = new HashSet<>();

            for (OrderItem item : o.getItems()) {
                Kitchen k = item.getKitchen() != null ? item.getKitchen() :
                        (item.getProduct() != null ? item.getProduct().getKitchen() : null);

                if (k != null && map.containsKey(k.getId())) {
                    KitchenAgg agg = map.get(k.getId());
                    touchedKitchens.add(k.getId());

                    BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                    BigDecimal subtotal = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;

                    if (item.isVoided()) {
                        agg.itemsCancelled = agg.itemsCancelled.add(qty);
                    } else {
                        agg.itemsPrepared = agg.itemsPrepared.add(qty);
                        agg.revenue = agg.revenue.add(subtotal);
                    }
                }
            }

            for (UUID kId : touchedKitchens) {
                map.get(kId).ordersCount++;
            }
        }

        return map.values().stream().map(a -> ReportDto.KitchenPerformance.builder()
                .kitchenId(a.kitchenId)
                .kitchenName(a.kitchenName)
                .ordersCount(a.ordersCount)
                .itemsPrepared(a.itemsPrepared)
                .itemsCancelled(a.itemsCancelled)
                .revenue(a.revenue)
                .build()).collect(Collectors.toList());
    }

    private static class KitchenAgg {
        UUID kitchenId;
        String kitchenName;
        long ordersCount = 0;
        BigDecimal itemsPrepared = BigDecimal.ZERO;
        BigDecimal itemsCancelled = BigDecimal.ZERO;
        BigDecimal revenue = BigDecimal.ZERO;

        KitchenAgg(UUID kitchenId, String kitchenName) {
            this.kitchenId = kitchenId;
            this.kitchenName = kitchenName;
        }
    }

    // ==========================================
    // 7. STOCK REPORT (OMBOR HARAKATI / BALANCE)
    // ==========================================
    @Transactional(readOnly = true)
    public List<ReportDto.StockReportItem> getStockReport(UUID tenantId, Instant from, Instant to, UUID warehouseId) {
        List<InventoryItem> items = warehouseId != null
                ? inventoryItemRepository.findByTenantIdAndWarehouseIdAndDeletedAtIsNullOrderByNameAsc(tenantId, warehouseId)
                : inventoryItemRepository.findByTenantIdAndDeletedAtIsNullOrderByNameAsc(tenantId);

        List<ReportDto.StockReportItem> result = new ArrayList<>();

        for (InventoryItem item : items) {
            List<InventoryTransaction> allTxs = inventoryTxRepository.findByItemIdOrderByCreatedAtDesc(item.getId(), org.springframework.data.domain.Pageable.unpaged());

            BigDecimal openingStock = BigDecimal.ZERO;
            BigDecimal incoming = BigDecimal.ZERO;
            BigDecimal outgoing = BigDecimal.ZERO;
            BigDecimal salesConsumption = BigDecimal.ZERO;
            BigDecimal waste = BigDecimal.ZERO;
            BigDecimal adjustment = BigDecimal.ZERO;

            for (InventoryTransaction tx : allTxs) {
                Instant txTime = tx.getCreatedAt();
                BigDecimal qty = tx.getQuantity() != null ? tx.getQuantity() : BigDecimal.ZERO;

                if (txTime.isBefore(from)) {
                    openingStock = openingStock.add(qty);
                } else if (!txTime.isAfter(to)) {
                    if (tx.getType() == InventoryTransaction.TransactionType.PURCHASE || tx.getType() == InventoryTransaction.TransactionType.IN) {
                        incoming = incoming.add(qty.abs());
                    } else if (tx.getType() == InventoryTransaction.TransactionType.OUT) {
                        outgoing = outgoing.add(qty.abs());
                    } else if (tx.getType() == InventoryTransaction.TransactionType.SALE) {
                        salesConsumption = salesConsumption.add(qty.abs());
                    } else if (tx.getType() == InventoryTransaction.TransactionType.WASTE) {
                        waste = waste.add(qty.abs());
                    } else if (tx.getType() == InventoryTransaction.TransactionType.ADJUSTMENT) {
                        adjustment = adjustment.add(qty);
                    }
                }
            }

            // Formula: Closing Stock = Opening Stock + Incoming - Outgoing - Sales Consumption - Waste +/- Adjustment
            BigDecimal closingStock = openingStock.add(incoming).subtract(outgoing).subtract(salesConsumption).subtract(waste).add(adjustment);
            // Agar tranzaksiyalardan oldingi stock 0 bo'lsa, joriy item qoldig'i bilan solishtirish
            if (allTxs.isEmpty()) {
                closingStock = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                openingStock = closingStock;
            }

            BigDecimal unitCost = item.getCostPrice() != null ? item.getCostPrice() : BigDecimal.ZERO;
            BigDecimal valuation = closingStock.multiply(unitCost);

            result.add(ReportDto.StockReportItem.builder()
                    .itemId(item.getId())
                    .itemName(item.getName())
                    .unit(item.getUnit())
                    .category(item.getCategory() != null ? item.getCategory() : "Umumiy")
                    .warehouseName(item.getWarehouse() != null ? item.getWarehouse().getName() : "Asosiy ombor")
                    .openingStock(openingStock)
                    .incoming(incoming)
                    .outgoing(outgoing)
                    .salesConsumption(salesConsumption)
                    .waste(waste)
                    .adjustment(adjustment)
                    .closingStock(closingStock)
                    .unitCost(unitCost)
                    .totalValuation(valuation)
                    .build());
        }

        return result;
    }

    // ==========================================
    // 8. CSV EXPORT
    // ==========================================
    @Transactional(readOnly = true)
    public String exportToCsv(String reportType, UUID tenantId, Instant from, Instant to, UUID waiterId, UUID kitchenId, UUID warehouseId, UserPrincipal principal) {
        StringBuilder csv = new StringBuilder();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(TASHKENT_ZONE);

        csv.append("Oybek Restaurant POS - Hisobot\n");
        csv.append("Hisobot turi:,").append(reportType).append("\n");
        csv.append("Davr:,").append(dtf.format(from)).append(" - ").append(dtf.format(to)).append("\n");
        csv.append("Yaratilgan vaqt:,").append(dtf.format(Instant.now())).append("\n");
        csv.append("Yaratuvchi:,").append(principal != null ? principal.getFullName() : "Admin").append("\n\n");

        if ("PRODUCTS".equalsIgnoreCase(reportType)) {
            csv.append("Mahsulot,Kategoriya,Sotilgan miqdor,Tushum,Tannarx,Foyda,Marja %\n");
            List<ReportDto.ProductSaleItem> items = getProductSalesReport(tenantId, from, to, null, kitchenId);
            for (ReportDto.ProductSaleItem i : items) {
                csv.append(escape(i.getProductName())).append(",")
                        .append(escape(i.getCategoryName())).append(",")
                        .append(i.getQuantity()).append(",")
                        .append(i.getRevenue()).append(",")
                        .append(i.getCost()).append(",")
                        .append(i.getProfit()).append(",")
                        .append(i.getProfitMargin()).append("%\n");
            }
        } else if ("STOCK".equalsIgnoreCase(reportType)) {
            csv.append("Mahsulot,Birlik,Ombor,Boshlang'ich qoldiq,Kirim,Chiqim,Sotuv sarfi,Isrof,Tuzatish,Yakuniy qoldiq,Tannarx,Ombor qiymati\n");
            List<ReportDto.StockReportItem> items = getStockReport(tenantId, from, to, warehouseId);
            for (ReportDto.StockReportItem i : items) {
                csv.append(escape(i.getItemName())).append(",")
                        .append(i.getUnit()).append(",")
                        .append(escape(i.getWarehouseName())).append(",")
                        .append(i.getOpeningStock()).append(",")
                        .append(i.getIncoming()).append(",")
                        .append(i.getOutgoing()).append(",")
                        .append(i.getSalesConsumption()).append(",")
                        .append(i.getWaste()).append(",")
                        .append(i.getAdjustment()).append(",")
                        .append(i.getClosingStock()).append(",")
                        .append(i.getUnitCost()).append(",")
                        .append(i.getTotalValuation()).append("\n");
            }
        } else if ("WAITERS".equalsIgnoreCase(reportType)) {
            csv.append("Ofitsiant,Buyurtmalar soni,Jami savdo,O'rtacha chek,Yetkazilgan taomlar,Bekor qilingan\n");
            List<ReportDto.WaiterPerformance> items = getWaiterReport(tenantId, from, to, waiterId, principal);
            for (ReportDto.WaiterPerformance i : items) {
                csv.append(escape(i.getWaiterName())).append(",")
                        .append(i.getOrdersCount()).append(",")
                        .append(i.getTotalSales()).append(",")
                        .append(i.getAvgCheck()).append(",")
                        .append(i.getDeliveredCount()).append(",")
                        .append(i.getCancelledCount()).append("\n");
            }
        } else if ("KITCHEN".equalsIgnoreCase(reportType)) {
            csv.append("Oshxona,Buyurtmalar soni,Tayyorlangan taomlar,Bekor qilingan taomlar,Tushum\n");
            List<ReportDto.KitchenPerformance> items = getKitchenReport(tenantId, from, to, kitchenId);
            for (ReportDto.KitchenPerformance i : items) {
                csv.append(escape(i.getKitchenName())).append(",")
                        .append(i.getOrdersCount()).append(",")
                        .append(i.getItemsPrepared()).append(",")
                        .append(i.getItemsCancelled()).append(",")
                        .append(i.getRevenue()).append("\n");
            }
        } else {
            // SAVDO / GENERAL
            ReportDto.SalesSummary s = getSalesSummary(tenantId, from, to, waiterId, kitchenId);
            csv.append("Ko'rsatkich,Qiymat\n");
            csv.append("Jami savdo,").append(s.getTotalSales()).append("\n");
            csv.append("Jami buyurtmalar,").append(s.getTotalOrders()).append("\n");
            csv.append("Jami to'lov,").append(s.getTotalPayments()).append("\n");
            csv.append("O'rtacha chek,").append(s.getAvgCheck()).append("\n");
            csv.append("Naqd to'lov,").append(s.getCashTotal()).append("\n");
            csv.append("Karta to'lov,").append(s.getCardTotal()).append("\n");
            csv.append("Boshqa to'lovlar,").append(s.getOtherTotal()).append("\n");
            csv.append("Bekor qilingan buyurtmalar,").append(s.getCancelledOrdersCount()).append("\n");
            csv.append("Qaytarilgan summa,").append(s.getRefundedAmount()).append("\n");
        }

        return csv.toString();
    }

    private String escape(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    // ==========================================
    // LEGACY METHODS (BACKWARD COMPATIBILITY)
    // ==========================================
    @Transactional(readOnly = true)
    public ReportDto.DailySummary getDailySummary(UUID tenantId, LocalDate date) {
        Instant from = date.atStartOfDay(TASHKENT_ZONE).toInstant();
        Instant to   = date.plusDays(1).atStartOfDay(TASHKENT_ZONE).toInstant();

        ReportDto.SalesSummary summary = getSalesSummary(tenantId, from, to, null, null);
        List<ReportDto.ProductSaleItem> topProdItems = getProductSalesReport(tenantId, from, to, null, null);

        List<ReportDto.TopProduct> topProducts = topProdItems.stream()
                .limit(10)
                .map(p -> ReportDto.TopProduct.builder()
                        .productName(p.getProductName())
                        .quantity(p.getQuantity().longValue())
                        .revenue(p.getRevenue())
                        .build())
                .collect(Collectors.toList());

        return ReportDto.DailySummary.builder()
                .date(from)
                .totalRevenue(summary.getTotalSales())
                .cashRevenue(summary.getCashTotal())
                .cardRevenue(summary.getCardTotal())
                .totalRefunds(summary.getRefundedAmount())
                .totalOrders(summary.getTotalOrders())
                .completedOrders(summary.getTotalOrders())
                .refundedOrders(summary.getRefundedOrdersCount())
                .avgOrderValue(summary.getAvgCheck())
                .topProducts(topProducts)
                .hourlyRevenue(summary.getHourlySales())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ReportDto.ShiftSummary> getShiftReports(UUID tenantId, LocalDate date) {
        Instant from = date.atStartOfDay(TASHKENT_ZONE).toInstant();
        Instant to   = date.plusDays(1).atStartOfDay(TASHKENT_ZONE).toInstant();

        return shiftRepository.findByTenantIdAndOpenedAtBetween(tenantId, from, to).stream()
                .map(s -> ReportDto.ShiftSummary.builder()
                        .shiftId(s.getId())
                        .cashierName(s.getCashier() != null
                                ? s.getCashier().getFirstName() + " " +
                                (s.getCashier().getLastName() != null ? s.getCashier().getLastName() : "")
                                : "Unknown")
                        .openedAt(s.getOpenedAt())
                        .closedAt(s.getClosedAt())
                        .totalSales(s.getTotalSales())
                        .totalCash(s.getTotalCashSales())
                        .totalCard(s.getTotalCardSales())
                        .totalRefunds(s.getTotalRefunds())
                        .ordersCount(s.getOrdersCount())
                        .build())
                .collect(Collectors.toList());
    }
}
