package com.restaurantpos.delivery.service;

import com.restaurantpos.common.exception.PosException;
import com.restaurantpos.delivery.dto.DeliveryMappingDto;
import com.restaurantpos.delivery.entity.DeliveryCategoryMappingEntity;
import com.restaurantpos.delivery.entity.DeliveryProductMappingEntity;
import com.restaurantpos.delivery.entity.DeliveryProviderEntity;
import com.restaurantpos.delivery.repository.DeliveryCategoryMappingRepository;
import com.restaurantpos.delivery.repository.DeliveryProductMappingRepository;
import com.restaurantpos.delivery.repository.DeliveryProviderRepository;
import com.restaurantpos.products.entity.Category;
import com.restaurantpos.products.entity.Product;
import com.restaurantpos.products.repository.CategoryRepository;
import com.restaurantpos.products.repository.ProductRepository;
import com.restaurantpos.tenants.entity.Tenant;
import com.restaurantpos.tenants.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryMappingService {

    private final DeliveryProductMappingRepository productMappingRepository;
    private final DeliveryCategoryMappingRepository categoryMappingRepository;
    private final DeliveryProviderRepository providerRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public Page<DeliveryMappingDto.ProductMappingResponse> getProductMappings(UUID tenantId, Pageable pageable) {
        return productMappingRepository.findByTenantId(tenantId, pageable).map(this::toProductResponse);
    }

    @Transactional(readOnly = true)
    public List<DeliveryMappingDto.ProductMappingResponse> getProductMappingsByProvider(UUID tenantId, UUID providerId) {
        return productMappingRepository.findByTenantIdAndProviderId(tenantId, providerId).stream()
                .map(this::toProductResponse)
                .toList();
    }

    @Transactional
    public DeliveryMappingDto.ProductMappingResponse mapProduct(UUID tenantId, DeliveryMappingDto.MapProductRequest req) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant topilmadi"));

        DeliveryProviderEntity provider = providerRepository.findByIdAndTenantIdAndDeletedAtIsNull(req.getProviderId(), tenantId)
                .orElseThrow(() -> PosException.notFound("Delivery provayder topilmadi: " + req.getProviderId()));

        Product product = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(req.getPosProductId(), tenantId)
                .orElseThrow(() -> PosException.notFound("POS mahsuloti topilmadi: " + req.getPosProductId()));

        DeliveryProductMappingEntity entity = productMappingRepository
                .findByProviderIdAndExternalProductId(req.getProviderId(), req.getExternalProductId())
                .orElseGet(() -> {
                    DeliveryProductMappingEntity m = new DeliveryProductMappingEntity();
                    m.setTenant(tenant);
                    m.setProvider(provider);
                    m.setExternalProductId(req.getExternalProductId());
                    return m;
                });

        entity.setExternalProductName(req.getExternalProductName());
        entity.setPosProduct(product);
        entity.setAutoMapped(false);

        DeliveryProductMappingEntity saved = productMappingRepository.save(entity);
        log.info("Mapped Delivery Product [{}: {}] -> POS Product [{}: {}]",
                provider.getCode(), req.getExternalProductId(), product.getId(), product.getName());
        return toProductResponse(saved);
    }

    @Transactional
    public void deleteProductMapping(UUID tenantId, UUID id) {
        DeliveryProductMappingEntity entity = productMappingRepository.findById(id)
                .filter(m -> m.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> PosException.notFound("Mapping topilmadi: " + id));
        productMappingRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public Page<DeliveryMappingDto.CategoryMappingResponse> getCategoryMappings(UUID tenantId, Pageable pageable) {
        return categoryMappingRepository.findByTenantId(tenantId, pageable).map(this::toCategoryResponse);
    }

    @Transactional
    public DeliveryMappingDto.CategoryMappingResponse mapCategory(UUID tenantId, DeliveryMappingDto.MapCategoryRequest req) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> PosException.notFound("Tenant topilmadi"));

        DeliveryProviderEntity provider = providerRepository.findByIdAndTenantIdAndDeletedAtIsNull(req.getProviderId(), tenantId)
                .orElseThrow(() -> PosException.notFound("Delivery provayder topilmadi: " + req.getProviderId()));

        Category category = categoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(req.getPosCategoryId(), tenantId)
                .orElseThrow(() -> PosException.notFound("POS kategoriyasi topilmadi: " + req.getPosCategoryId()));

        DeliveryCategoryMappingEntity entity = categoryMappingRepository
                .findByProviderIdAndExternalCategoryId(req.getProviderId(), req.getExternalCategoryId())
                .orElseGet(() -> {
                    DeliveryCategoryMappingEntity m = new DeliveryCategoryMappingEntity();
                    m.setTenant(tenant);
                    m.setProvider(provider);
                    m.setExternalCategoryId(req.getExternalCategoryId());
                    return m;
                });

        entity.setExternalCategoryName(req.getExternalCategoryName());
        entity.setPosCategory(category);

        DeliveryCategoryMappingEntity saved = categoryMappingRepository.save(entity);
        return toCategoryResponse(saved);
    }

    @Transactional
    public void deleteCategoryMapping(UUID tenantId, UUID id) {
        DeliveryCategoryMappingEntity entity = categoryMappingRepository.findById(id)
                .filter(m -> m.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> PosException.notFound("Mapping topilmadi: " + id));
        categoryMappingRepository.delete(entity);
    }

    @Transactional
    public Optional<Product> resolveProduct(DeliveryProviderEntity provider, String externalProductId, String externalProductName) {
        // 1. Direct mapping lookup
        Optional<DeliveryProductMappingEntity> mappingOpt = productMappingRepository
                .findByProviderIdAndExternalProductId(provider.getId(), externalProductId);
        if (mappingOpt.isPresent()) {
            return Optional.of(mappingOpt.get().getPosProduct());
        }

        // 2. Auto-match by name or SKU
        if (externalProductName != null && !externalProductName.isBlank()) {
            String trimmed = externalProductName.trim();
            List<Product> products = productRepository.findByTenantIdAndDeletedAtIsNullOrderBySortOrderAscNameAsc(provider.getTenant().getId());
            for (Product p : products) {
                if (p.getName().equalsIgnoreCase(trimmed) || trimmed.equalsIgnoreCase(p.getSku())) {
                    // Create auto-mapping
                    DeliveryProductMappingEntity autoMapping = new DeliveryProductMappingEntity();
                    autoMapping.setTenant(provider.getTenant());
                    autoMapping.setProvider(provider);
                    autoMapping.setExternalProductId(externalProductId);
                    autoMapping.setExternalProductName(externalProductName);
                    autoMapping.setPosProduct(p);
                    autoMapping.setAutoMapped(true);
                    productMappingRepository.save(autoMapping);
                    log.info("Auto-mapped external product '{}' ({}) to POS product '{}'",
                            externalProductName, externalProductId, p.getName());
                    return Optional.of(p);
                }
            }
        }

        return Optional.empty();
    }

    private DeliveryMappingDto.ProductMappingResponse toProductResponse(DeliveryProductMappingEntity m) {
        return DeliveryMappingDto.ProductMappingResponse.builder()
                .id(m.getId())
                .providerId(m.getProvider().getId())
                .providerName(m.getProvider().getName())
                .externalProductId(m.getExternalProductId())
                .externalProductName(m.getExternalProductName())
                .posProductId(m.getPosProduct().getId())
                .posProductName(m.getPosProduct().getName())
                .posProductCategory(m.getPosProduct().getCategory() != null ? m.getPosProduct().getCategory().getName() : null)
                .autoMapped(m.isAutoMapped())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private DeliveryMappingDto.CategoryMappingResponse toCategoryResponse(DeliveryCategoryMappingEntity m) {
        return DeliveryMappingDto.CategoryMappingResponse.builder()
                .id(m.getId())
                .providerId(m.getProvider().getId())
                .providerName(m.getProvider().getName())
                .externalCategoryId(m.getExternalCategoryId())
                .externalCategoryName(m.getExternalCategoryName())
                .posCategoryId(m.getPosCategory().getId())
                .posCategoryName(m.getPosCategory().getName())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
