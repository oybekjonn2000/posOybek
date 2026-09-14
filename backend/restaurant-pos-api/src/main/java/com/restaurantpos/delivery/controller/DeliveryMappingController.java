package com.restaurantpos.delivery.controller;

import com.restaurantpos.auth.security.UserPrincipal;
import com.restaurantpos.common.config.PaginationUtils;
import com.restaurantpos.common.response.ApiResponse;
import com.restaurantpos.delivery.dto.DeliveryMappingDto;
import com.restaurantpos.delivery.service.DeliveryMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/delivery/mappings")
@RequiredArgsConstructor
@Tag(name = "Delivery Mappings", description = "Product and Category Mappings API")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class DeliveryMappingController {

    private final DeliveryMappingService mappingService;

    @GetMapping("/products")
    @Operation(summary = "Get product mappings with pagination")
    public ResponseEntity<ApiResponse<List<DeliveryMappingDto.ProductMappingResponse>>> getProductMappings(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "25") Integer size,
            @AuthenticationPrincipal UserPrincipal user) {
        Pageable pageable = PaginationUtils.safePageable(page, size);
        Page<DeliveryMappingDto.ProductMappingResponse> result = mappingService.getProductMappings(user.getTenantId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), ApiResponse.PageMeta.of(result)));
    }

    @PostMapping("/products")
    @Operation(summary = "Create or update product mapping")
    public ResponseEntity<ApiResponse<DeliveryMappingDto.ProductMappingResponse>> mapProduct(
            @Valid @RequestBody DeliveryMappingDto.MapProductRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(
                mappingService.mapProduct(user.getTenantId(), request), "Mahsulot muvaffaqiyatli bog'landi"));
    }

    @DeleteMapping("/products/{id}")
    @Operation(summary = "Delete product mapping")
    public ResponseEntity<ApiResponse<Void>> deleteProductMapping(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        mappingService.deleteProductMapping(user.getTenantId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Mapping o'chirildi"));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get category mappings with pagination")
    public ResponseEntity<ApiResponse<List<DeliveryMappingDto.CategoryMappingResponse>>> getCategoryMappings(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "25") Integer size,
            @AuthenticationPrincipal UserPrincipal user) {
        Pageable pageable = PaginationUtils.safePageable(page, size);
        Page<DeliveryMappingDto.CategoryMappingResponse> result = mappingService.getCategoryMappings(user.getTenantId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), ApiResponse.PageMeta.of(result)));
    }

    @PostMapping("/categories")
    @Operation(summary = "Create or update category mapping")
    public ResponseEntity<ApiResponse<DeliveryMappingDto.CategoryMappingResponse>> mapCategory(
            @Valid @RequestBody DeliveryMappingDto.MapCategoryRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(
                mappingService.mapCategory(user.getTenantId(), request), "Kategoriya muvaffaqiyatli bog'landi"));
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Delete category mapping")
    public ResponseEntity<ApiResponse<Void>> deleteCategoryMapping(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        mappingService.deleteCategoryMapping(user.getTenantId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Mapping o'chirildi"));
    }
}
