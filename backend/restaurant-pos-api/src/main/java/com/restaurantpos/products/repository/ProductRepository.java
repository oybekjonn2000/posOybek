package com.restaurantpos.products.repository;

import com.restaurantpos.products.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByTenantIdAndDeletedAtIsNullOrderBySortOrderAscNameAsc(UUID tenantId);

    List<Product> findByTenantIdAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAscNameAsc(UUID tenantId);

    List<Product> findByTenantIdAndActiveTrueAndAvailableTrueAndDeletedAtIsNullOrderBySortOrderAscNameAsc(UUID tenantId);

    List<Product> findByTenantIdAndCategoryIdAndDeletedAtIsNullOrderBySortOrderAscNameAsc(UUID tenantId, UUID categoryId);

    List<Product> findByTenantIdAndCategoryIdAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAscNameAsc(UUID tenantId, UUID categoryId);

    List<Product> findByTenantIdAndCategoryIdAndActiveTrueAndAvailableTrueAndDeletedAtIsNullOrderBySortOrderAscNameAsc(UUID tenantId, UUID categoryId);

    long countByTenantIdAndCategoryIdAndDeletedAtIsNull(UUID tenantId, UUID categoryId);

    long countByTenantIdAndKitchenIdAndDeletedAtIsNull(UUID tenantId, UUID kitchenId);

    Optional<Product> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<Product> findByTenantIdAndBarcodeAndDeletedAtIsNull(UUID tenantId, String barcode);

    Optional<Product> findByTenantIdAndSkuAndDeletedAtIsNull(UUID tenantId, String sku);

    @Query("SELECT p FROM Product p WHERE p.tenant.id = :tenantId AND p.deletedAt IS NULL AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.nameUz) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.nameRu) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "p.sku LIKE CONCAT('%', :query, '%') OR " +
           "p.barcode LIKE CONCAT('%', :query, '%')) " +
           "ORDER BY p.name ASC")
    List<Product> searchProducts(@Param("tenantId") UUID tenantId, @Param("query") String query);

    @Query(value = "SELECT p FROM Product p WHERE p.tenant.id = :tenantId AND p.deletedAt IS NULL AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:activeOnly = false OR (p.active = true AND p.available = true)) AND " +
           "(:query IS NULL OR :query = '' OR " +
           " LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(p.nameUz) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(p.nameRu) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " p.sku LIKE CONCAT('%', :query, '%') OR " +
           " p.barcode LIKE CONCAT('%', :query, '%')) " +
           "ORDER BY p.sortOrder ASC, p.name ASC",
           countQuery = "SELECT count(p) FROM Product p WHERE p.tenant.id = :tenantId AND p.deletedAt IS NULL AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:activeOnly = false OR (p.active = true AND p.available = true)) AND " +
           "(:query IS NULL OR :query = '' OR " +
           " LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(p.nameUz) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(p.nameRu) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " p.sku LIKE CONCAT('%', :query, '%') OR " +
           " p.barcode LIKE CONCAT('%', :query, '%'))")
    org.springframework.data.domain.Page<Product> findProductsPaginated(
            @Param("tenantId") UUID tenantId,
            @Param("categoryId") UUID categoryId,
            @Param("query") String query,
            @Param("activeOnly") boolean activeOnly,
            org.springframework.data.domain.Pageable pageable);
}
