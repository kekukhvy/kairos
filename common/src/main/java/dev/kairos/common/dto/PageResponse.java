package dev.kairos.common.dto;

import java.util.List;

/**
 * Paginated list response. Wraps any item type with the pagination window that
 * produced it, so clients can implement next-page logic without extra calls.
 */
public record PageResponse<T>(
        List<T> items,
        int limit,
        int offset,
        boolean hasNext) {
}
