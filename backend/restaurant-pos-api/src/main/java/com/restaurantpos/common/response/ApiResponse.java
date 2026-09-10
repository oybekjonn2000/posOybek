package com.restaurantpos.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Standard API response wrapper for all endpoints.
 * Ensures consistent response format across the entire application.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;
    private final String errorCode;
    private final String requestId;

    @Builder.Default
    private final Instant timestamp = Instant.now();

    // Pagination metadata
    private final PageMeta page;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .requestId(UUID.randomUUID().toString())
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .requestId(UUID.randomUUID().toString())
                .build();
    }

    public static <T> ApiResponse<T> success(T data, PageMeta page) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .page(page)
                .requestId(UUID.randomUUID().toString())
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .requestId(UUID.randomUUID().toString())
                .build();
    }

    @Getter
    @Builder
    public static class PageMeta {
        private final int page;
        private final int size;
        private final long totalElements;
        private final int totalPages;
        private final boolean first;
        private final boolean last;

        public static PageMeta of(org.springframework.data.domain.Page<?> page) {
            return PageMeta.builder()
                    .page(page.getNumber())
                    .size(page.getSize())
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .first(page.isFirst())
                    .last(page.isLast())
                    .build();
        }
    }
}
