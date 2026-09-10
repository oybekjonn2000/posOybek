package com.restaurantpos.tables.controller;

import com.restaurantpos.auth.security.UserPrincipal;
import com.restaurantpos.common.response.ApiResponse;
import com.restaurantpos.tables.dto.TableDto;
import com.restaurantpos.tables.service.TableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
@Tag(name = "Tables", description = "Floor Plan & Tables API")
public class TableController {

    private final TableService tableService;

    @GetMapping("/zones")
    @Operation(summary = "Get all dining zones")
    public ResponseEntity<ApiResponse<List<TableDto.ZoneResponse>>> getZones(
            @AuthenticationPrincipal UserPrincipal user) {
        List<TableDto.ZoneResponse> zones = tableService.getZones(user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(zones));
    }

    @PostMapping("/zones")
    @PreAuthorize("hasAuthority('MANAGE_TABLES')")
    @Operation(summary = "Create dining zone (Zal, Ko'cha, Ayvon, Podval, etc.)")
    public ResponseEntity<ApiResponse<TableDto.ZoneResponse>> createZone(
            @Valid @RequestBody TableDto.CreateZoneRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        TableDto.ZoneResponse created = tableService.createZone(user.getTenantId(), request);
        return ResponseEntity.ok(ApiResponse.success(created, "Zona muvaffaqiyatli yaratildi"));
    }

    @GetMapping
    @Operation(summary = "Get all tables or filter by zone")
    public ResponseEntity<ApiResponse<List<TableDto.Response>>> getTables(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) UUID zoneId) {
        List<TableDto.Response> tables = tableService.getTables(user.getTenantId(), zoneId, user);
        return ResponseEntity.ok(ApiResponse.success(tables));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get table by ID")
    public ResponseEntity<ApiResponse<TableDto.Response>> getTable(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        TableDto.Response table = tableService.getTableById(id, user.getTenantId(), user);
        return ResponseEntity.ok(ApiResponse.success(table));
    }

    @PostMapping("/{id}/occupy")
    @PreAuthorize("hasAnyAuthority('CREATE_ORDER', 'ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_WAITER')")
    @Operation(summary = "Occupy table and bind to current waiter")
    public ResponseEntity<ApiResponse<TableDto.Response>> occupyTable(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        TableDto.Response table = tableService.occupyTable(id, user.getTenantId(), user);
        return ResponseEntity.ok(ApiResponse.success(table, "Stol muvaffaqiyatli egallandi"));
    }


    @PutMapping("/{id}/status")
    @Operation(summary = "Update table status (FREE, OCCUPIED, RESERVED, BILL_REQUESTED, CLEANING)")
    public ResponseEntity<ApiResponse<TableDto.Response>> updateTableStatus(
            @PathVariable UUID id,
            @Valid @RequestBody TableDto.UpdateStatusRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        TableDto.Response updated = tableService.updateTableStatus(id, user.getTenantId(), request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Table status updated"));
    }

    @PutMapping("/{id}/layout")
    @PreAuthorize("hasAuthority('MANAGE_TABLES')")
    @Operation(summary = "Update table position and dimensions on floor plan (Drag & Drop)")
    public ResponseEntity<ApiResponse<TableDto.Response>> updateTableLayout(
            @PathVariable UUID id,
            @RequestBody TableDto.UpdateLayoutRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        TableDto.Response updated = tableService.updateTableLayout(id, user.getTenantId(), request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Table layout updated"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_TABLES')")
    @Operation(summary = "Create new table")
    public ResponseEntity<ApiResponse<TableDto.Response>> createTable(
            @Valid @RequestBody TableDto.CreateRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        TableDto.Response created = tableService.createTable(user.getTenantId(), request);
        return ResponseEntity.ok(ApiResponse.success(created, "Table created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_TABLES')")
    @Operation(summary = "Update table details")
    public ResponseEntity<ApiResponse<TableDto.Response>> updateTable(
            @PathVariable UUID id,
            @RequestBody TableDto.UpdateRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        TableDto.Response updated = tableService.updateTable(id, user.getTenantId(), request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Table updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_TABLES')")
    @Operation(summary = "Delete table")
    public ResponseEntity<ApiResponse<Void>> deleteTable(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        tableService.deleteTable(id, user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(null, "Table deleted successfully"));
    }
}
