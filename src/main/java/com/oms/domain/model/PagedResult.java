package com.oms.domain.model;

import java.util.List;

/**
 * Represents a paginated result in the domain.
 * Avoids coupling the application/domain layer to Spring Data (org.springframework.data.domain.Page).
 */
public record PagedResult<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
