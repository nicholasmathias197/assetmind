package com.assetmind.core.domain;

import java.util.List;

public record PaginatedResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public PaginatedResult(List<T> content, int page, int size, long totalElements) {
        this(content, page, size, totalElements, (int) Math.ceil((double) totalElements / size));
    }
}

