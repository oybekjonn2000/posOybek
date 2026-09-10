package com.restaurantpos.users.repository;

import com.restaurantpos.users.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsernameAndDeletedAtIsNull(String username);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);

    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId AND u.deletedAt IS NULL")
    Page<User> findAllByTenantId(UUID tenantId, Pageable pageable);

    boolean existsByUsernameAndTenantIdAndDeletedAtIsNull(String username, UUID tenantId);
}
