package com.restaurantpos.inventory.repository;

import com.restaurantpos.inventory.entity.ProductIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductIngredientRepository extends JpaRepository<ProductIngredient, UUID> {
    List<ProductIngredient> findByProductId(UUID productId);
    List<ProductIngredient> findByInventoryItemId(UUID inventoryItemId);
    Optional<ProductIngredient> findByProductIdAndInventoryItemId(UUID productId, UUID inventoryItemId);
    void deleteByProductId(UUID productId);
}
