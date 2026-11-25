package com.parkingspot.domain.model.history;

import java.util.List;

/** Una página de resultados con el total para la paginación básica. */
public record PageResult<T>(List<T> items, int page, int size, long totalItems) {

    public PageResult {
        items = List.copyOf(items);
    }

    public int totalPages() {
        return (int) ((totalItems + size - 1) / size);
    }
}
