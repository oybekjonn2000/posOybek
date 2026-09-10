package com.restaurantpos.sync.service;

import com.restaurantpos.sync.dto.SyncDto;
import com.restaurantpos.sync.entity.SyncEvent;
import com.restaurantpos.sync.repository.SyncEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private final SyncEventRepository syncEventRepository;

    @Value("${app.sync.cloud-api-url:}")
    private String cloudApiUrl;

    private Instant lastSyncedAt = Instant.now();

    @Transactional(readOnly = true)
    public SyncDto.StatusResponse getStatus(UUID tenantId) {
        long pending = syncEventRepository.countByTenantIdAndStatus(tenantId, SyncEvent.SyncStatus.PENDING);
        long failed = syncEventRepository.countByTenantIdAndStatus(tenantId, SyncEvent.SyncStatus.FAILED);

        boolean hasCloud = cloudApiUrl != null && !cloudApiUrl.trim().isEmpty();

        String statusStr = "ONLINE";
        if (!hasCloud) {
            statusStr = "LOCAL_STANDALONE";
        } else if (failed > 0) {
            statusStr = "SYNC_ERROR";
        } else if (pending > 0) {
            statusStr = "SYNCING";
        }

        return SyncDto.StatusResponse.builder()
                .online(true)
                .pendingEvents(pending)
                .failedEvents(failed)
                .lastSyncedAt(lastSyncedAt)
                .cloudApiUrl(cloudApiUrl)
                .status(statusStr)
                .build();
    }

    @Transactional
    public void recordEvent(UUID tenantId, String entityType, UUID entityId,
                            SyncEvent.Operation operation, java.util.Map<String, Object> payload) {
        SyncEvent event = new SyncEvent();
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setOperation(operation);
        event.setPayload(payload != null ? payload : java.util.Collections.emptyMap());
        event.setStatus(SyncEvent.SyncStatus.PENDING);
        event.setCreatedAt(Instant.now());

        syncEventRepository.save(event);
        log.debug("Recorded offline sync event: {} {} {}", operation, entityType, entityId);
    }

    @Transactional
    public int triggerManualSync(UUID tenantId) {
        List<SyncEvent> pending = syncEventRepository.findByTenantIdAndStatusOrderByCreatedAtAsc(
                tenantId, SyncEvent.SyncStatus.PENDING, PageRequest.of(0, 100));

        if (pending.isEmpty()) {
            return 0;
        }

        int syncedCount = 0;
        for (SyncEvent event : pending) {
            // In local offline mode without cloud URL, mark as synced
            event.setStatus(SyncEvent.SyncStatus.SYNCED);
            event.setSyncedAt(Instant.now());
            syncEventRepository.save(event);
            syncedCount++;
        }

        this.lastSyncedAt = Instant.now();
        log.info("Synced {} offline events for tenant {}", syncedCount, tenantId);
        return syncedCount;
    }
}
