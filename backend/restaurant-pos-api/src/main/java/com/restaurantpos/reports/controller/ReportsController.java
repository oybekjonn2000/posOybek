package com.restaurantpos.reports.controller;

import com.restaurantpos.auth.security.UserPrincipal;
import com.restaurantpos.common.response.ApiResponse;
import com.restaurantpos.reports.dto.ReportDto;
import com.restaurantpos.reports.service.ReportsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Real-time analytics va hisobotlar API")
public class ReportsController {

    private final ReportsService reportsService;
    private static final ZoneId TASHKENT_ZONE = ZoneId.of("Asia/Tashkent");

    private Instant[] parseDateRange(LocalDate dateFrom, LocalDate dateTo) {
        LocalDate fromDate = dateFrom != null ? dateFrom : LocalDate.now();
        LocalDate toDate = dateTo != null ? dateTo : fromDate;
        Instant from = fromDate.atStartOfDay(TASHKENT_ZONE).toInstant();
        Instant to = toDate.plusDays(1).atStartOfDay(TASHKENT_ZONE).toInstant();
        return new Instant[]{from, to};
    }

    // ==========================================
    // 1. SAVDO HISOBOTI (SALES SUMMARY)
    // ==========================================
    @GetMapping("/sales")
    @PreAuthorize("hasAuthority('VIEW_REPORTS') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Umumiy savdo hisoboti")
    public ResponseEntity<ApiResponse<ReportDto.SalesSummary>> getSalesSummary(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) UUID waiterId,
            @RequestParam(required = false) UUID kitchenId) {
        Instant[] range = parseDateRange(dateFrom, dateTo);
        return ResponseEntity.ok(ApiResponse.success(
                reportsService.getSalesSummary(user.getTenantId(), range[0], range[1], waiterId, kitchenId)));
    }

    // ==========================================
    // 2. MAHSULOT SAVDO HISOBOTI (PRODUCT SALES)
    // ==========================================
    @GetMapping("/products")
    @PreAuthorize("hasAuthority('VIEW_REPORTS') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Taomlar va mahsulotlar savdo hisoboti")
    public ResponseEntity<ApiResponse<List<ReportDto.ProductSaleItem>>> getProductSales(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID kitchenId) {
        Instant[] range = parseDateRange(dateFrom, dateTo);
        return ResponseEntity.ok(ApiResponse.success(
                reportsService.getProductSalesReport(user.getTenantId(), range[0], range[1], categoryId, kitchenId)));
    }

    // ==========================================
    // 3. FOYDA HISOBOTI (PROFIT & LOSS)
    // ==========================================
    @GetMapping("/profit")
    @PreAuthorize("hasAuthority('VIEW_REPORTS') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Foyda va zarar hisoboti (P&L)")
    public ResponseEntity<ApiResponse<ReportDto.ProfitLoss>> getProfitLoss(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        Instant[] range = parseDateRange(dateFrom, dateTo);
        return ResponseEntity.ok(ApiResponse.success(
                reportsService.getProfitLossReport(user.getTenantId(), range[0], range[1])));
    }

    // ==========================================
    // 4. KASSA HISOBOTI (CASHIER SUMMARY)
    // ==========================================
    @GetMapping("/cashier")
    @PreAuthorize("hasAuthority('VIEW_REPORTS') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Kassa va to'lov turlari hisoboti")
    public ResponseEntity<ApiResponse<List<ReportDto.CashierSummary>>> getCashierReport(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        Instant[] range = parseDateRange(dateFrom, dateTo);
        return ResponseEntity.ok(ApiResponse.success(
                reportsService.getCashierReport(user.getTenantId(), range[0], range[1])));
    }

    // ==========================================
    // 5. OFITSIANT HISOBOTI (WAITER PERFORMANCE)
    // ==========================================
    @GetMapping("/waiters")
    @PreAuthorize("hasAuthority('VIEW_REPORTS') or hasAuthority('VIEW_DASHBOARD') or hasRole('WAITER')")
    @Operation(summary = "Ofitsiantlar samaradorligi hisoboti")
    public ResponseEntity<ApiResponse<List<ReportDto.WaiterPerformance>>> getWaiterReport(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) UUID waiterId) {
        Instant[] range = parseDateRange(dateFrom, dateTo);
        return ResponseEntity.ok(ApiResponse.success(
                reportsService.getWaiterReport(user.getTenantId(), range[0], range[1], waiterId, user)));
    }

    // ==========================================
    // 6. OSHXONA HISOBOTI (KITCHEN PERFORMANCE)
    // ==========================================
    @GetMapping("/kitchens")
    @PreAuthorize("hasAuthority('VIEW_REPORTS') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Oshxonalar samaradorligi hisoboti")
    public ResponseEntity<ApiResponse<List<ReportDto.KitchenPerformance>>> getKitchenReport(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) UUID kitchenId) {
        Instant[] range = parseDateRange(dateFrom, dateTo);
        return ResponseEntity.ok(ApiResponse.success(
                reportsService.getKitchenReport(user.getTenantId(), range[0], range[1], kitchenId)));
    }

    // ==========================================
    // 7. STOCK REPORT (OMBOR HARAKATI / BALANCE)
    // ==========================================
    @GetMapping("/stock")
    @PreAuthorize("hasAuthority('VIEW_REPORTS') or hasAuthority('VIEW_DASHBOARD') or hasAuthority('INVENTORY_VIEW')")
    @Operation(summary = "Ombor harakati va yakuniy qoldiq hisoboti")
    public ResponseEntity<ApiResponse<List<ReportDto.StockReportItem>>> getStockReport(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) UUID warehouseId) {
        Instant[] range = parseDateRange(dateFrom, dateTo);
        return ResponseEntity.ok(ApiResponse.success(
                reportsService.getStockReport(user.getTenantId(), range[0], range[1], warehouseId)));
    }

    // ==========================================
    // 8. CSV EXPORT
    // ==========================================
    @GetMapping("/export/csv")
    @PreAuthorize("hasAuthority('VIEW_REPORTS') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Hisobotni CSV formatida eksport qilish")
    public ResponseEntity<byte[]> exportCsv(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "SALES") String reportType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) UUID waiterId,
            @RequestParam(required = false) UUID kitchenId,
            @RequestParam(required = false) UUID warehouseId) {
        Instant[] range = parseDateRange(dateFrom, dateTo);
        String csv = reportsService.exportToCsv(reportType, user.getTenantId(), range[0], range[1], waiterId, kitchenId, warehouseId, user);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report_" + reportType.toLowerCase() + ".csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(bytes);
    }

    // ==========================================
    // LEGACY (BACKWARD COMPATIBILITY)
    // ==========================================
    @GetMapping("/daily")
    @PreAuthorize("hasAuthority('VIEW_REPORTS') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Kunlik savdo hisoboti")
    public ResponseEntity<ApiResponse<ReportDto.DailySummary>> daily(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate reportDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(
                reportsService.getDailySummary(user.getTenantId(), reportDate)));
    }

    @GetMapping("/shifts")
    @PreAuthorize("hasAuthority('VIEW_REPORTS') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Smena hisobotlari")
    public ResponseEntity<ApiResponse<List<ReportDto.ShiftSummary>>> shifts(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate reportDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(
                reportsService.getShiftReports(user.getTenantId(), reportDate)));
    }
}
