package com.restaurantpos.sync.repository;

import com.restaurantpos.sync.entity.SyncEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SyncEventRepository extends JpaRepository<SyncEvent, UUID> {

    List<SyncEvent> findByTenantIdAndStatusOrderByCreatedAtAsc(UUID tenantId, SyncEvent.SyncStatus status, Pageable pageable);

    long countByTenantIdAndStatus(UUID tenantId, SyncEvent.SyncStatus status);

    List<SyncEvent> findByTenantIdAndStatusAndNextRetryAtBeforeOrderByCreatedAtAsc(
            UUID tenantId, SyncEvent.SyncStatus status, Instant now, Pageable pageable);
}
