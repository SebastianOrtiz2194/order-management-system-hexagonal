package com.oms.domain.model;

import java.util.List;

/**
 * Representa un resultado paginado en el dominio.
 * Evita acoplar la capa de aplicación/dominio a Spring Data (org.springframework.data.domain.Page).
 */
public record PagedResult<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
