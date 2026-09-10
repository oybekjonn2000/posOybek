package com.restaurantpos.products.controller;

import com.restaurantpos.auth.security.UserPrincipal;
import com.restaurantpos.common.response.ApiResponse;
import com.restaurantpos.products.dto.CategoryDto;
import com.restaurantpos.products.service.CategoryService;
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
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Product Categories API")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all categories, optionally filtered by kitchenId")
    public ResponseEntity<ApiResponse<List<CategoryDto.Response>>> getCategories(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
            @RequestParam(required = false) UUID kitchenId) {
        List<CategoryDto.Response> categories = categoryService.getAllCategories(user.getTenantId(), activeOnly, kitchenId);
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<ApiResponse<CategoryDto.Response>> getCategory(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        CategoryDto.Response category = categoryService.getCategoryById(id, user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_CATEGORIES')")
    @Operation(summary = "Create category")
    public ResponseEntity<ApiResponse<CategoryDto.Response>> createCategory(
            @Valid @RequestBody CategoryDto.CreateRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        CategoryDto.Response created = categoryService.createCategory(user.getTenantId(), request);
        return ResponseEntity.ok(ApiResponse.success(created, "Category created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_CATEGORIES')")
    @Operation(summary = "Update category")
    public ResponseEntity<ApiResponse<CategoryDto.Response>> updateCategory(
            @PathVariable UUID id,
            @RequestBody CategoryDto.UpdateRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        CategoryDto.Response updated = categoryService.updateCategory(id, user.getTenantId(), request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Category updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_CATEGORIES')")
    @Operation(summary = "Delete category (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        categoryService.deleteCategory(id, user.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(null, "Category deleted successfully"));
    }
}
