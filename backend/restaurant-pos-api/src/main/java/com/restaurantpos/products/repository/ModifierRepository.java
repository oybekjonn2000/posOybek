package com.restaurantpos.products.repository;

import com.restaurantpos.products.entity.Modifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ModifierRepository extends JpaRepository<Modifier, UUID> {

    List<Modifier> findByGroupIdAndDeletedAtIsNullOrderBySortOrderAsc(UUID groupId);
}
