package com.restaurantpos.reports.controller;

import com.restaurantpos.auth.security.UserPrincipal;
import com.restaurantpos.common.response.ApiResponse;
import com.restaurantpos.reports.dto.ReportDto;
import com.restaurantpos.reports.service.ReportsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Savdo hisobotlari API")
public class ReportsController {

    private final ReportsService reportsService;

    @GetMapping("/daily")
    @PreAuthorize("hasAuthority('VIEW_REPORTS') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Kunlik savdo hisoboti")
    public ResponseEntity<ApiResponse<ReportDto.DailySummary>> daily(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate reportDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(
                reportsService.getDailySummary(user.getTenantId(), reportDate)));
    }

    @GetMapping("/shifts")
    @PreAuthorize("hasAuthority('VIEW_REPORTS') or hasAuthority('VIEW_DASHBOARD')")
    @Operation(summary = "Smena hisobotlari")
    public ResponseEntity<ApiResponse<List<ReportDto.ShiftSummary>>> shifts(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate reportDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(
                reportsService.getShiftReports(user.getTenantId(), reportDate)));
    }
}
