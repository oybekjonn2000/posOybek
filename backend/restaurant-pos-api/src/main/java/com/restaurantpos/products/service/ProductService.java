package com.restaurantpos.products.service;

import com.restaurantpos.common.exception.PosException;
import com.restaurantpos.products.dto.ProductDto;
import com.restaurantpos.products.entity.Category;
import com.restaurantpos.products.entity.Modifier;
import com.restaurantpos.products.entity.ModifierGroup;
import com.restaurantpos.products.entity.Product;
import com.restaurantpos.products.repository.CategoryRepository;
import com.restaurantpos.products.repository.ModifierGroupRepository;
import com.restaurantpos.products.repository.ProductRepository;
import com.restaurantpos.tenants.entity.Tenant;
import com.restaurantpos.tenants.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final TenantRepository tenantRepository;
    private final ModifierGroupRepository modifierGroupRepository;
    private final com.restaurantpos.kitchen.repository.KitchenRepository kitchenRepository;

    @Transactional(readOnly = true)
    public List<ProductDto.Response> getProducts(UUID tenantId, UUID categoryId, String query, boolean activeOnly) {
        List<Product> products;

        if (query != null && !query.trim().isEmpty()) {
            products = productRepository.searchProducts(tenantId, query.trim());
        } else if (categoryId != null) {
            products = productRepository.findByTenantIdAndCategoryIdAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAscNameAsc(tenantId, categoryId);
        } else if (activeOnly) {
            products = productRepository.findByTenantIdAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAscNameAsc(tenantId);
        } else {
            products = productRepository.findByTenantIdAndDeletedAtIsNullOrderBySortOrderAscNameAsc(tenantId);
        }

        return products.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDto.Response getProductById(UUID id, UUID tenantId) {
        Product product = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Product not found: " + id));
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductDto.Response getProductByBarcode(String barcode, UUID tenantId) {
        Product product = productRepository.findByTenantIdAndBarcodeAndDeletedAtIsNull(tenantId, barcode)
                .orElseThrow(() -> PosException.notFound("Product not found with barcode: " + barcode));
        return toResponse(product);
    }

    @Transactional
    public ProductDto.Response createProduct(UUID tenantId, ProductDto.CreateRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant not found"));

        if (request.getCategoryId() == null) {
            throw PosException.badRequest("Kategoriya tanlanishi shart. Mahsulot faqat kategoriya orqali oshxonaga bog'lanadi.");
        }

        Category category = categoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(request.getCategoryId(), tenantId)
                .orElseThrow(() -> PosException.badRequest("Tanlangan kategoriya topilmadi"));

        if (!category.isActive()) {
            throw PosException.badRequest("Tanlangan kategoriya faol emas. Faqat faol kategoriyaga mahsulot qo'shish mumkin.");
        }

        if (category.getKitchen() == null) {
            throw PosException.badRequest("Tanlangan kategoriyaga oshxona biriktirilmagan");
        }

        Product product = new Product();
        product.setTenant(tenant);
        product.setCategory(category); // Automatically sets product.kitchen = category.getKitchen()
        product.setSku(request.getSku());
        product.setBarcode(request.getBarcode());
        product.setName(request.getName());
        product.setNameUz(request.getNameUz());
        product.setNameRu(request.getNameRu());
        product.setNameEn(request.getNameEn());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        product.setUnit(request.getUnit() != null ? request.getUnit() : "piece");
        product.setPurchasePrice(request.getPurchasePrice());
        product.setSalePrice(request.getSalePrice());
        product.setTaxRate(request.getTaxRate());
        product.setTaxable(request.isTaxable());
        product.setActive(request.isActive());
        product.setAvailable(request.isAvailable());
        product.setTrackStock(request.isTrackStock());
        product.setMinStockLevel(request.getMinStockLevel());
        product.setSortOrder(request.getSortOrder());

        if (request.getModifierGroupIds() != null && !request.getModifierGroupIds().isEmpty()) {
            List<ModifierGroup> groups = modifierGroupRepository.findAllById(request.getModifierGroupIds());
            product.setModifierGroups(new java.util.HashSet<>(groups));
        }

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @Transactional
    public ProductDto.Response updateProduct(UUID id, UUID tenantId, ProductDto.UpdateRequest request) {
        Product product = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Product not found: " + id));

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(request.getCategoryId(), tenantId)
                    .orElseThrow(() -> PosException.badRequest("Tanlangan kategoriya topilmadi"));
            if (!category.isActive()) {
                throw PosException.badRequest("Tanlangan kategoriya faol emas.");
            }
            if (category.getKitchen() == null) {
                throw PosException.badRequest("Tanlangan kategoriyaga oshxona biriktirilmagan");
            }
            product.setCategory(category); // Automatically updates product.kitchen to match the new category's kitchen!
        }

        if (request.getSku() != null) product.setSku(request.getSku());
        if (request.getBarcode() != null) product.setBarcode(request.getBarcode());
        if (request.getName() != null) product.setName(request.getName());
        if (request.getNameUz() != null) product.setNameUz(request.getNameUz());
        if (request.getNameRu() != null) product.setNameRu(request.getNameRu());
        if (request.getNameEn() != null) product.setNameEn(request.getNameEn());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getImageUrl() != null) product.setImageUrl(request.getImageUrl());
        if (request.getUnit() != null) product.setUnit(request.getUnit());
        if (request.getPurchasePrice() != null) product.setPurchasePrice(request.getPurchasePrice());
        if (request.getSalePrice() != null) product.setSalePrice(request.getSalePrice());
        if (request.getTaxRate() != null) product.setTaxRate(request.getTaxRate());
        if (request.getTaxable() != null) product.setTaxable(request.getTaxable());
        if (request.getActive() != null) product.setActive(request.getActive());
        if (request.getAvailable() != null) product.setAvailable(request.getAvailable());
        if (request.getTrackStock() != null) product.setTrackStock(request.getTrackStock());
        if (request.getMinStockLevel() != null) product.setMinStockLevel(request.getMinStockLevel());
        if (request.getSortOrder() != null) product.setSortOrder(request.getSortOrder());

        if (request.getModifierGroupIds() != null) {
            List<ModifierGroup> groups = modifierGroupRepository.findAllById(request.getModifierGroupIds());
            product.setModifierGroups(new java.util.HashSet<>(groups));
        }

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @Transactional
    public void deleteProduct(UUID id, UUID tenantId) {
        Product product = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> PosException.notFound("Product not found: " + id));
        product.softDelete();
        productRepository.save(product);
    }

    public ProductDto.Response toResponse(Product product) {
        List<ProductDto.ModifierGroupResponse> modifierGroups = Collections.emptyList();
        if (product.getModifierGroups() != null) {
            modifierGroups = product.getModifierGroups().stream().map(g -> {
                List<ProductDto.ModifierResponse> modifiers = Collections.emptyList();
                if (g.getModifiers() != null) {
                    modifiers = g.getModifiers().stream().map(m -> ProductDto.ModifierResponse.builder()
                            .id(m.getId())
                            .name(m.getName())
                            .price(m.getPrice())
                            .sortOrder(m.getSortOrder())
                            .build()
                    ).collect(Collectors.toList());
                }
                return ProductDto.ModifierGroupResponse.builder()
                        .id(g.getId())
                        .name(g.getName())
                        .description(g.getDescription())
                        .required(g.isRequired())
                        .minSelections(g.getMinSelections())
                        .maxSelections(g.getMaxSelections())
                        .modifiers(modifiers)
                        .build();
            }).collect(Collectors.toList());
        }

        return ProductDto.Response.builder()
                .id(product.getId())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .kitchenId(product.getKitchen() != null ? product.getKitchen().getId() : null)
                .kitchenName(product.getKitchen() != null ? product.getKitchen().getName() : null)
                .sku(product.getSku())
                .barcode(product.getBarcode())
                .name(product.getName())
                .nameUz(product.getNameUz())
                .nameRu(product.getNameRu())
                .nameEn(product.getNameEn())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .unit(product.getUnit())
                .purchasePrice(product.getPurchasePrice())
                .salePrice(product.getSalePrice())
                .taxRate(product.getTaxRate())
                .taxable(product.isTaxable())
                .active(product.isActive())
                .available(product.isAvailable())
                .trackStock(product.isTrackStock())
                .minStockLevel(product.getMinStockLevel())
                .currentStock(product.getCurrentStock())
                .sortOrder(product.getSortOrder())
                .version(product.getVersion())
                .modifierGroups(modifierGroups)
                .createdAt(product.getCreatedAt())
                .build();
    }
}
