package com.restaurantpos.products.service;

import com.restaurantpos.common.exception.PosException;
import com.restaurantpos.products.dto.CategoryDto;
import com.restaurantpos.products.entity.Category;
import com.restaurantpos.products.repository.CategoryRepository;
import com.restaurantpos.tenants.entity.Tenant;
import com.restaurantpos.tenants.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public List<CategoryDto.Response> getAllCategories(UUID tenantId, boolean activeOnly) {
        List<Category> categories = activeOnly
                ? categoryRepository.findByTenantIdAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc(tenantId)
                : categoryRepository.findByTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(tenantId);

        return categories.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDto.Response getCategoryById(UUID id, UUID tenantId) {
        Category category = categoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Category not found: " + id));
        return toResponse(category);
    }

    @Transactional
    public CategoryDto.Response createCategory(UUID tenantId, CategoryDto.CreateRequest request) {
        if (categoryRepository.existsByTenantIdAndNameIgnoreCaseAndDeletedAtIsNull(tenantId, request.getName())) {
            throw PosException.badRequest("Category with name '" + request.getName() + "' already exists");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant not found"));

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(request.getParentId(), tenantId)
                    .orElseThrow(() -> PosException.notFound("Parent category not found"));
        }

        Category category = new Category();
        category.setTenant(tenant);
        category.setName(request.getName());
        category.setNameUz(request.getNameUz());
        category.setNameRu(request.getNameRu());
        category.setNameEn(request.getNameEn());
        category.setDescription(request.getDescription());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor());
        category.setImageUrl(request.getImageUrl());
        category.setSortOrder(request.getSortOrder());
        category.setParent(parent);
        category.setActive(true);

        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Transactional
    public CategoryDto.Response updateCategory(UUID id, UUID tenantId, CategoryDto.UpdateRequest request) {
        Category category = categoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Category not found: " + id));

        if (request.getName() != null) category.setName(request.getName());
        if (request.getNameUz() != null) category.setNameUz(request.getNameUz());
        if (request.getNameRu() != null) category.setNameRu(request.getNameRu());
        if (request.getNameEn() != null) category.setNameEn(request.getNameEn());
        if (request.getDescription() != null) category.setDescription(request.getDescription());
        if (request.getIcon() != null) category.setIcon(request.getIcon());
        if (request.getColor() != null) category.setColor(request.getColor());
        if (request.getImageUrl() != null) category.setImageUrl(request.getImageUrl());
        if (request.getSortOrder() != null) category.setSortOrder(request.getSortOrder());
        if (request.getActive() != null) category.setActive(request.getActive());

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(request.getParentId(), tenantId)
                    .orElseThrow(() -> PosException.notFound("Parent category not found"));
            category.setParent(parent);
        }

        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Transactional
    public void deleteCategory(UUID id, UUID tenantId) {
        Category category = categoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Category not found: " + id));
        category.softDelete();
        categoryRepository.save(category);
    }

    private CategoryDto.Response toResponse(Category category) {
        return CategoryDto.Response.builder()
                .id(category.getId())
                .name(category.getName())
                .nameUz(category.getNameUz())
                .nameRu(category.getNameRu())
                .nameEn(category.getNameEn())
                .description(category.getDescription())
                .icon(category.getIcon())
                .color(category.getColor())
                .imageUrl(category.getImageUrl())
                .sortOrder(category.getSortOrder())
                .active(category.isActive())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .createdAt(category.getCreatedAt())
                .build();
    }
}
