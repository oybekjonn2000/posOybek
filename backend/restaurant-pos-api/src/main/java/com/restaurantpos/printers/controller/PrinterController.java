package com.restaurantpos.printers.controller;

import com.restaurantpos.auth.security.UserPrincipal;
import com.restaurantpos.common.response.ApiResponse;
import com.restaurantpos.printers.dto.PrinterDto;
import com.restaurantpos.printers.service.PrintRoutingService;
import com.restaurantpos.printers.service.PrinterService;
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
@RequestMapping("/api/printers")
@RequiredArgsConstructor
@Tag(name = "Printers", description = "Printer Management & Ticket Routing API")
public class PrinterController {

    private final PrinterService printerService;
    private final PrintRoutingService printRoutingService;

    @GetMapping
    @Operation(summary = "Get all configured printers for current tenant")
    public ResponseEntity<ApiResponse<List<PrinterDto.Response>>> getPrinters(
            @AuthenticationPrincipal UserPrincipal user) {
        List<PrinterDto.Response> printers = printerService.getPrinters(user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(printers));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get printer by ID")
    public ResponseEntity<ApiResponse<PrinterDto.Response>> getPrinter(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        PrinterDto.Response printer = printerService.getPrinterById(user.getTenantId(), id);
        return ResponseEntity.ok(ApiResponse.success(printer));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_SETTINGS') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Register new printer")
    public ResponseEntity<ApiResponse<PrinterDto.Response>> createPrinter(
            @Valid @RequestBody PrinterDto.CreateRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        PrinterDto.Response created = printerService.createPrinter(user.getTenantId(), user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(created, "Printer muvaffaqiyatli qo'shildi"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_SETTINGS') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Update printer configuration")
    public ResponseEntity<ApiResponse<PrinterDto.Response>> updatePrinter(
            @PathVariable UUID id,
            @RequestBody PrinterDto.UpdateRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        PrinterDto.Response updated = printerService.updatePrinter(user.getTenantId(), user.getUserId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Printer muvaffaqiyatli yangilandi"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_SETTINGS') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Deactivate/delete printer")
    public ResponseEntity<ApiResponse<Void>> deletePrinter(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        printerService.deletePrinter(user.getTenantId(), user.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Printer o'chirildi"));
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "Send test print job to printer with real connection verification")
    public ResponseEntity<ApiResponse<PrinterDto.TestPrintResult>> testPrint(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        PrinterDto.TestPrintResult result = printerService.testPrint(user.getTenantId(), id);
        if (result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
        } else {
            return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
        }
    }

    @PostMapping("/reprint/kitchen-ticket/{ticketId}")
    @PreAuthorize("hasAuthority('EDIT_ORDER') or hasAuthority('CREATE_ORDER') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Reprint existing kitchen ticket without modifying order state")
    public ResponseEntity<ApiResponse<Void>> reprintKitchenTicket(
            @PathVariable UUID ticketId,
            @AuthenticationPrincipal UserPrincipal user) {
        printRoutingService.reprintKitchenTicket(user.getTenantId(), ticketId);
        return ResponseEntity.ok(ApiResponse.success(null, "Oshxona ticketi qayta chop etishga yuborildi"));
    }

    @PostMapping("/reprint/order-receipt/{orderId}")
    @PreAuthorize("hasAuthority('PROCESS_PAYMENT') or hasRole('ADMIN') or hasRole('CASHIER') or hasRole('MANAGER')")
    @Operation(summary = "Reprint existing order receipt without duplicate billing")
    public ResponseEntity<ApiResponse<Void>> reprintOrderReceipt(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal user) {
        printRoutingService.reprintOrderReceipt(user.getTenantId(), orderId);
        return ResponseEntity.ok(ApiResponse.success(null, "Kassa cheki qayta chop etishga yuborildi"));
    }
}
