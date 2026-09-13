package com.restaurantpos.common.config;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Utility for building safe Spring Data Pageable instances.
 * Enforces maximum page size limits (default max 100) to protect against DoS.
 */
public final class PaginationUtils {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 100;

    private PaginationUtils() {}

    /**
     * Creates a safe Pageable with clamped page size (1..100).
     */
    public static Pageable safePageable(Integer page, Integer size) {
        return safePageable(page, size, Sort.unsorted());
    }

    /**
     * Creates a safe Pageable with clamped page size (1..100) and specified Sort.
     */
    public static Pageable safePageable(Integer page, Integer size, Sort sort) {
        int p = (page != null && page >= 0) ? page : DEFAULT_PAGE;
        int s = (size != null && size > 0) ? Math.min(size, MAX_SIZE) : DEFAULT_SIZE;
        Sort st = (sort != null) ? sort : Sort.unsorted();
        return PageRequest.of(p, s, st);
    }
}
