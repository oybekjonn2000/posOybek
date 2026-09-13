package com.restaurantpos.inventory.controller;

import com.restaurantpos.auth.security.UserPrincipal;
import com.restaurantpos.common.response.ApiResponse;
import com.restaurantpos.inventory.dto.InventoryDto;
import com.restaurantpos.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Ombor va zaxira boshqaruvi API")
public class InventoryController {

    private final InventoryService inventoryService;

    // ==========================================
    // 1. DASHBOARD
    // ==========================================
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW') or hasAuthority('VIEW_DASHBOARD') or hasAuthority('VIEW_REPORTS')")
    @Operation(summary = "Ombor dashboard statistikasi")
    public ResponseEntity<ApiResponse<InventoryDto.DashboardStats>> getDashboard(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getDashboardStats(user.getTenantId())));
    }

    // ==========================================
    // 2. ITEMS CRUD
    // ==========================================
    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_STOCK') or hasAuthority('INVENTORY_VIEW') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Ombor mahsulotlari ro'yxati (filterlar bilan)")
    public ResponseEntity<ApiResponse<List<InventoryDto.Response>>> getAll(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        List<InventoryDto.Response> items = inventoryService.getItemsFiltered(user.getTenantId(), warehouseId, category, lowStock);
        if (page != null) {
            org.springframework.data.domain.Pageable pageable = com.restaurantpos.common.config.PaginationUtils.safePageable(page, size);
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), items.size());
            List<InventoryDto.Response> content = (start <= end && start < items.size())
                    ? items.subList(start, end)
                    : java.util.Collections.emptyList();
            org.springframework.data.domain.Page<InventoryDto.Response> pageResult =
                    new org.springframework.data.domain.PageImpl<>(content, pageable, items.size());
            return ResponseEntity.ok(ApiResponse.success(pageResult.getContent(), ApiResponse.PageMeta.of(pageResult)));
        }
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAuthority('VIEW_STOCK') or hasAuthority('INVENTORY_VIEW') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Kam qolgan mahsulotlar")
    public ResponseEntity<ApiResponse<List<InventoryDto.Response>>> getLowStock(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getLowStockItems(user.getTenantId())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_STOCK') or hasAuthority('INVENTORY_VIEW') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Ombor mahsuloti tafsiloti")
    public ResponseEntity<ApiResponse<InventoryDto.Response>> getById(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getItem(user.getTenantId(), id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_STOCK') or hasAuthority('INVENTORY_MANAGE') or hasAuthority('MANAGE_PRODUCTS')")
    @Operation(summary = "Yangi ombor mahsuloti qo'shish")
    public ResponseEntity<ApiResponse<InventoryDto.Response>> create(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody InventoryDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(inventoryService.createItem(user.getTenantId(), request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_STOCK') or hasAuthority('INVENTORY_MANAGE') or hasAuthority('MANAGE_PRODUCTS')")
    @Operation(summary = "Ombor mahsulotini tahrirlash")
    public ResponseEntity<ApiResponse<InventoryDto.Response>> update(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id,
            @RequestBody InventoryDto.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.updateItem(user.getTenantId(), id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_STOCK') or hasAuthority('INVENTORY_MANAGE') or hasAuthority('MANAGE_PRODUCTS')")
    @Operation(summary = "Ombor mahsulotini o'chirish")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id) {
        inventoryService.deleteItem(user.getTenantId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Mahsulot o'chirildi"));
    }

    // ==========================================
    // 3. STOCK ADJUST & OUTBOUND
    // ==========================================
    @PatchMapping("/{id}/adjust")
    @PreAuthorize("hasAuthority('MANAGE_STOCK') or hasAuthority('INVENTORY_MANAGE') or hasAuthority('MANAGE_PRODUCTS')")
    @Operation(summary = "Ombor miqdorini tuzatish (Adjust)")
    public ResponseEntity<ApiResponse<InventoryDto.Response>> adjust(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id,
            @RequestBody InventoryDto.AdjustRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                inventoryService.adjustStock(user.getTenantId(), id, request, user.getUserId())));
    }

    @PostMapping("/outbound")
    @PreAuthorize("hasAuthority('MANAGE_STOCK') or hasAuthority('INVENTORY_MANAGE') or hasAuthority('MANAGE_PRODUCTS')")
    @Operation(summary = "Ombordan chiqim qilish (Oshxona, buzilgan, yaroqlilik)")
    public ResponseEntity<ApiResponse<InventoryDto.Response>> outbound(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody InventoryDto.OutboundRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                inventoryService.recordOutbound(user.getTenantId(), request, user.getUserId())));
    }

    // ==========================================
    // 4. MOVEMENTS / HARAKATLAR
    // ==========================================
    @GetMapping("/transactions")
    @PreAuthorize("hasAuthority('VIEW_STOCK') or hasAuthority('INVENTORY_VIEW') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Barcha harakatlar tarixi")
    public ResponseEntity<ApiResponse<List<InventoryDto.TransactionResponse>>> getAllTransactions(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) UUID itemId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        ZoneId zone = ZoneId.of("Asia/Tashkent");
        Instant from = dateFrom != null ? dateFrom.atStartOfDay(zone).toInstant() : null;
        Instant to = dateTo != null ? dateTo.plusDays(1).atStartOfDay(zone).toInstant() : null;

        return ResponseEntity.ok(ApiResponse.success(
                inventoryService.getTransactions(user.getTenantId(), itemId, type, warehouseId, from, to, page, size)));
    }

    @GetMapping("/{id}/transactions")
    @PreAuthorize("hasAuthority('VIEW_STOCK') or hasAuthority('INVENTORY_VIEW') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Muayyan mahsulot harakatlari tarixi")
    public ResponseEntity<ApiResponse<List<InventoryDto.TransactionResponse>>> getItemTransactions(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                inventoryService.getTransactions(user.getTenantId(), id, null, null, null, null, page, size)));
    }

    // ==========================================
    // 5. KIRIM / PURCHASES
    // ==========================================
    @GetMapping("/purchases")
    @PreAuthorize("hasAuthority('VIEW_STOCK') or hasAuthority('INVENTORY_VIEW') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Barcha kirim hujjatlari ro'yxati")
    public ResponseEntity<ApiResponse<List<InventoryDto.PurchaseResponse>>> getPurchases(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllPurchases(user.getTenantId())));
    }

    @PostMapping("/purchases")
    @PreAuthorize("hasAuthority('MANAGE_STOCK') or hasAuthority('INVENTORY_MANAGE') or hasAuthority('MANAGE_PRODUCTS')")
    @Operation(summary = "Yangi kirim qilish")
    public ResponseEntity<ApiResponse<InventoryDto.PurchaseResponse>> createPurchase(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody InventoryDto.PurchaseCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(inventoryService.createPurchase(user.getTenantId(), request, user.getUserId())));
    }

    // ==========================================
    // 6. INVENTARIZATSIYA / AUDITS
    // ==========================================
    @GetMapping("/audits")
    @PreAuthorize("hasAuthority('VIEW_STOCK') or hasAuthority('INVENTORY_VIEW') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Inventarizatsiyalar ro'yxati")
    public ResponseEntity<ApiResponse<List<InventoryDto.AuditResponse>>> getAudits(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllAudits(user.getTenantId())));
    }

    @GetMapping("/audits/{id}")
    @PreAuthorize("hasAuthority('VIEW_STOCK') or hasAuthority('INVENTORY_VIEW') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Inventarizatsiya tafsiloti")
    public ResponseEntity<ApiResponse<InventoryDto.AuditResponse>> getAudit(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAuditDetails(user.getTenantId(), id)));
    }

    @PostMapping("/audits/start")
    @PreAuthorize("hasAuthority('MANAGE_STOCK') or hasAuthority('INVENTORY_MANAGE') or hasAuthority('MANAGE_PRODUCTS')")
    @Operation(summary = "Yangi inventarizatsiya boshlash")
    public ResponseEntity<ApiResponse<InventoryDto.AuditResponse>> startAudit(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody(required = false) InventoryDto.AuditStartRequest request) {
        InventoryDto.AuditStartRequest req = request != null ? request : new InventoryDto.AuditStartRequest();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(inventoryService.startAudit(user.getTenantId(), req, user.getUserId())));
    }

    @PostMapping("/audits/{id}/submit")
    @PreAuthorize("hasAuthority('MANAGE_STOCK') or hasAuthority('INVENTORY_MANAGE') or hasAuthority('MANAGE_PRODUCTS')")
    @Operation(summary = "Inventarizatsiya natijalarini tasdiqlash va stock tuzatish")
    public ResponseEntity<ApiResponse<InventoryDto.AuditResponse>> submitAudit(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id,
            @RequestBody InventoryDto.AuditSubmitRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                inventoryService.submitAudit(user.getTenantId(), id, request, user.getUserId())));
    }

    // ==========================================
    // 7. RECIPES / RETSEPTLAR
    // ==========================================
    @GetMapping("/recipes")
    @PreAuthorize("hasAuthority('VIEW_STOCK') or hasAuthority('INVENTORY_VIEW') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Taom retseptlari ro'yxati")
    public ResponseEntity<ApiResponse<List<InventoryDto.ProductRecipeResponse>>> getAllRecipes(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllRecipes(user.getTenantId())));
    }

    @GetMapping("/recipes/product/{productId}")
    @PreAuthorize("hasAuthority('VIEW_STOCK') or hasAuthority('INVENTORY_VIEW') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Muayyan taom retsepti")
    public ResponseEntity<ApiResponse<InventoryDto.ProductRecipeResponse>> getRecipe(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getProductRecipe(user.getTenantId(), productId)));
    }

    @PostMapping("/recipes")
    @PreAuthorize("hasAuthority('MANAGE_STOCK') or hasAuthority('INVENTORY_MANAGE') or hasAuthority('MANAGE_PRODUCTS')")
    @Operation(summary = "Taom retseptini saqlash / yangilash")
    public ResponseEntity<ApiResponse<InventoryDto.ProductRecipeResponse>> saveRecipe(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody InventoryDto.SaveRecipeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.saveProductRecipe(user.getTenantId(), request)));
    }

    // ==========================================
    // 8. WAREHOUSES & SUPPLIERS
    // ==========================================
    @GetMapping("/warehouses")
    @PreAuthorize("hasAuthority('VIEW_STOCK') or hasAuthority('INVENTORY_VIEW') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Omborlar ro'yxati")
    public ResponseEntity<ApiResponse<List<InventoryDto.WarehouseResponse>>> getWarehouses(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllWarehouses(user.getTenantId())));
    }

    @PostMapping("/warehouses")
    @PreAuthorize("hasAuthority('MANAGE_STOCK') or hasAuthority('INVENTORY_MANAGE') or hasAuthority('MANAGE_PRODUCTS')")
    @Operation(summary = "Yangi ombor qo'shish")
    public ResponseEntity<ApiResponse<InventoryDto.WarehouseResponse>> createWarehouse(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody InventoryDto.WarehouseCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(inventoryService.createWarehouse(user.getTenantId(), request)));
    }

    @GetMapping("/suppliers")
    @PreAuthorize("hasAuthority('VIEW_STOCK') or hasAuthority('INVENTORY_VIEW') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Yetkazib beruvchilar ro'yxati")
    public ResponseEntity<ApiResponse<List<InventoryDto.SupplierResponse>>> getSuppliers(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllSuppliers(user.getTenantId())));
    }

    @PostMapping("/suppliers")
    @PreAuthorize("hasAuthority('MANAGE_STOCK') or hasAuthority('INVENTORY_MANAGE') or hasAuthority('MANAGE_PRODUCTS')")
    @Operation(summary = "Yangi yetkazib beruvchi qo'shish")
    public ResponseEntity<ApiResponse<InventoryDto.SupplierResponse>> createSupplier(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody InventoryDto.SupplierCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(inventoryService.createSupplier(user.getTenantId(), request)));
    }
}
