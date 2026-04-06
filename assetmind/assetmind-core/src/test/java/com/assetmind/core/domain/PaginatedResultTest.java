package com.assetmind.core.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaginatedResultTest {

    @Test
    void totalPagesCalculatedAutomatically() {
        PaginatedResult<String> result = new PaginatedResult<>(List.of("a", "b"), 0, 2, 10);
        assertEquals(5, result.totalPages());
    }

    @Test
    void totalPagesRoundsUp() {
        PaginatedResult<String> result = new PaginatedResult<>(List.of("a"), 0, 3, 10);
        assertEquals(4, result.totalPages());
    }

    @Test
    void singlePageResult() {
        PaginatedResult<String> result = new PaginatedResult<>(List.of("a", "b", "c"), 0, 10, 3);
        assertEquals(1, result.totalPages());
    }

    @Test
    void emptyResult() {
        PaginatedResult<String> result = new PaginatedResult<>(List.of(), 0, 10, 0);
        assertEquals(0, result.totalPages());
    }

    @Test
    void fiveArgConstructorPreservesAll() {
        PaginatedResult<String> result = new PaginatedResult<>(List.of("x"), 2, 5, 50, 10);
        assertEquals(List.of("x"), result.content());
        assertEquals(2, result.page());
        assertEquals(5, result.size());
        assertEquals(50, result.totalElements());
        assertEquals(10, result.totalPages());
    }
}
