package com.restaurantpos.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class SyncDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusResponse {
        private boolean online;
        private long pendingEvents;
        private long failedEvents;
        private Instant lastSyncedAt;
        private String cloudApiUrl;
        private String status; // ONLINE, OFFLINE, SYNCING, ERROR
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventResponse {
        private UUID id;
        private String entityType;
        private UUID entityId;
        private String operation;
        private Map<String, Object> payload;
        private long version;
        private String status;
        private int retryCount;
        private String lastError;
        private Instant createdAt;
    }
}
