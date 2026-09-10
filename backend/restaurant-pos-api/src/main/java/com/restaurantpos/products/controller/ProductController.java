package com.restaurantpos.products.controller;

import com.restaurantpos.auth.security.UserPrincipal;
import com.restaurantpos.common.response.ApiResponse;
import com.restaurantpos.products.dto.ProductDto;
import com.restaurantpos.products.service.ProductService;
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
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Products & Menu Items API")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Get products list with filtering and search")
    public ResponseEntity<ApiResponse<List<ProductDto.Response>>> getProducts(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<ProductDto.Response> products = productService.getProducts(user.getTenantId(), categoryId, query, activeOnly);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductDto.Response>> getProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        ProductDto.Response product = productService.getProductById(id, user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/barcode/{barcode}")
    @Operation(summary = "Get product by barcode (scanner support)")
    public ResponseEntity<ApiResponse<ProductDto.Response>> getProductByBarcode(
            @PathVariable String barcode,
            @AuthenticationPrincipal UserPrincipal user) {
        ProductDto.Response product = productService.getProductByBarcode(barcode, user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_PRODUCTS')")
    @Operation(summary = "Create new product")
    public ResponseEntity<ApiResponse<ProductDto.Response>> createProduct(
            @Valid @RequestBody ProductDto.CreateRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        ProductDto.Response created = productService.createProduct(user.getTenantId(), request);
        return ResponseEntity.ok(ApiResponse.success(created, "Product created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_PRODUCTS')")
    @Operation(summary = "Update existing product")
    public ResponseEntity<ApiResponse<ProductDto.Response>> updateProduct(
            @PathVariable UUID id,
            @RequestBody ProductDto.UpdateRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        ProductDto.Response updated = productService.updateProduct(id, user.getTenantId(), request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Product updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_PRODUCTS')")
    @Operation(summary = "Delete product (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        productService.deleteProduct(id, user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(null, "Product deleted successfully"));
    }
}
