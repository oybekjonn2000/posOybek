package com.restaurantpos.inventory.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class InventoryDto {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DashboardStats {
        private long totalProducts;
        private BigDecimal totalStockQuantity;
        private long lowStockCount;
        private long outOfStockCount;
        private BigDecimal todayIncomingAmount;
        private BigDecimal todayOutgoingAmount;
        private BigDecimal todaySalesConsumptionAmount;
        private BigDecimal warehouseValuation;
        private BigDecimal recentDiscrepancyAmount;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CreateRequest {
        private String name;
        private String sku;
        private String unit;
        private BigDecimal quantity;
        private BigDecimal minQuantity;
        private BigDecimal maxQuantity;
        private BigDecimal costPrice;
        private BigDecimal sellingPrice;
        private String category;
        private String notes;
        private UUID warehouseId;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class UpdateRequest {
        private String name;
        private String sku;
        private String unit;
        private BigDecimal minQuantity;
        private BigDecimal maxQuantity;
        private BigDecimal costPrice;
        private BigDecimal sellingPrice;
        private String category;
        private String notes;
        private UUID warehouseId;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private UUID id;
        private String name;
        private String sku;
        private String unit;
        private BigDecimal quantity;
        private BigDecimal minQuantity;
        private BigDecimal maxQuantity;
        private BigDecimal costPrice;
        private BigDecimal sellingPrice;
        private String category;
        private boolean lowStock;
        private boolean outOfStock;
        private UUID warehouseId;
        private String warehouseName;
        private boolean active;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AdjustRequest {
        private BigDecimal quantity;   // positive = qo'shish, negative = ayirish
        private String type;           // ADJUSTMENT, WASTE, OUT, IN
        private String notes;
        private BigDecimal unitCost;
        private UUID warehouseId;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class OutboundRequest {
        private UUID itemId;
        private UUID warehouseId;
        private BigDecimal quantity;
        private String reason; // Oshxona uchun, Buzilgan, Yaroqlilik muddati tugagan, Ichki foydalanish, Manual
        private String type;   // OUT or WASTE
        private String notes;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TransactionResponse {
        private UUID id;
        private UUID itemId;
        private String itemName;
        private String unit;
        private UUID warehouseId;
        private String warehouseName;
        private String type;
        private BigDecimal quantity;
        private BigDecimal quantityBefore;
        private BigDecimal quantityAfter;
        private BigDecimal unitCost;
        private BigDecimal totalCost;
        private String referenceType;
        private UUID referenceId;
        private String referenceNumber;
        private String notes;
        private String userName;
        private Instant createdAt;
    }

    // --- Kirim / Purchases ---
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PurchaseItemRequest {
        private UUID itemId;
        private BigDecimal quantity;
        private BigDecimal unitCost;
        private String notes;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PurchaseCreateRequest {
        private UUID supplierId;
        private UUID warehouseId;
        private String invoiceNumber;
        private LocalDate purchaseDate;
        private String notes;
        private BigDecimal paidAmount;
        private List<PurchaseItemRequest> items;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PurchaseItemResponse {
        private UUID id;
        private UUID itemId;
        private String itemName;
        private String itemSku;
        private String unit;
        private BigDecimal quantity;
        private BigDecimal unitCost;
        private BigDecimal totalCost;
        private String notes;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PurchaseResponse {
        private UUID id;
        private String purchaseNumber;
        private String invoiceNumber;
        private UUID supplierId;
        private String supplierName;
        private UUID warehouseId;
        private String warehouseName;
        private LocalDate purchaseDate;
        private String status;
        private BigDecimal totalAmount;
        private BigDecimal paidAmount;
        private BigDecimal balanceDue;
        private String notes;
        private List<PurchaseItemResponse> items;
        private Instant createdAt;
    }

    // --- Inventarizatsiya / Audits ---
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AuditStartRequest {
        private UUID warehouseId;
        private String title;
        private String notes;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AuditItemCountRequest {
        private UUID itemId;
        private BigDecimal actualQuantity;
        private String notes;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AuditSubmitRequest {
        private List<AuditItemCountRequest> items;
        private String notes;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AuditItemResponse {
        private UUID id;
        private UUID itemId;
        private String itemName;
        private String sku;
        private String unit;
        private BigDecimal systemQuantity;
        private BigDecimal actualQuantity;
        private BigDecimal difference;
        private BigDecimal unitCost;
        private BigDecimal totalDifferenceCost;
        private String notes;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AuditResponse {
        private UUID id;
        private String auditNumber;
        private UUID warehouseId;
        private String warehouseName;
        private String title;
        private String status;
        private BigDecimal totalDiscrepancyCost;
        private String conductedByName;
        private Instant startedAt;
        private Instant completedAt;
        private String notes;
        private List<AuditItemResponse> items;
    }

    // --- Retsept / Recipes ---
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RecipeItemRequest {
        private UUID inventoryItemId;
        private BigDecimal quantity;
        private String unit;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class SaveRecipeRequest {
        private UUID productId;
        private List<RecipeItemRequest> items;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RecipeItemResponse {
        private UUID id;
        private UUID inventoryItemId;
        private String inventoryItemName;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal costPrice;
        private BigDecimal totalCost;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProductRecipeResponse {
        private UUID productId;
        private String productName;
        private String categoryName;
        private BigDecimal price;
        private List<RecipeItemResponse> recipeItems;
        private BigDecimal totalCostPrice;
    }

    // --- Warehouses & Suppliers ---
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class WarehouseCreateRequest {
        private String name;
        private String description;
        private String address;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WarehouseResponse {
        private UUID id;
        private String name;
        private String description;
        private String address;
        private boolean active;
        private Instant createdAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class SupplierCreateRequest {
        private String name;
        private String contactPerson;
        private String phone;
        private String email;
        private String address;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SupplierResponse {
        private UUID id;
        private String name;
        private String contactPerson;
        private String phone;
        private String email;
        private String address;
        private BigDecimal totalPurchases;
        private BigDecimal totalPaid;
        private BigDecimal balanceDue;
        private boolean active;
        private Instant createdAt;
    }
}
