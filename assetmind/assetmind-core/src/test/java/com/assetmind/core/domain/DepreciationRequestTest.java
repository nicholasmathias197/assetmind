package com.assetmind.core.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DepreciationRequestTest {

    @Test
    void recordFieldAccess() {
        DepreciationRequest req = new DepreciationRequest(
                "a1", BookType.TAX, DepreciationMethod.MACRS_200DB_HY,
                AssetClass.VEHICLE, LocalDate.of(2026, 6, 1),
                new BigDecimal("30000.00"), new BigDecimal("5000.00"), 5,
                true, new BigDecimal("10000.00"), new BigDecimal("0.60")
        );

        assertEquals("a1", req.assetId());
        assertEquals(BookType.TAX, req.bookType());
        assertEquals(DepreciationMethod.MACRS_200DB_HY, req.method());
        assertEquals(AssetClass.VEHICLE, req.assetClass());
        assertEquals(LocalDate.of(2026, 6, 1), req.inServiceDate());
        assertEquals(new BigDecimal("30000.00"), req.costBasis());
        assertEquals(new BigDecimal("5000.00"), req.salvageValue());
        assertEquals(5, req.usefulLifeYears());
        assertTrue(req.section179Enabled());
        assertEquals(new BigDecimal("10000.00"), req.section179Amount());
        assertEquals(new BigDecimal("0.60"), req.bonusDepreciationRate());
    }

    @Test
    void equality() {
        DepreciationRequest a = new DepreciationRequest("x", BookType.BOOK, DepreciationMethod.STRAIGHT_LINE,
                AssetClass.OTHER, LocalDate.of(2026, 1, 1), BigDecimal.TEN, BigDecimal.ZERO, 5,
                false, BigDecimal.ZERO, BigDecimal.ZERO);
        DepreciationRequest b = new DepreciationRequest("x", BookType.BOOK, DepreciationMethod.STRAIGHT_LINE,
                AssetClass.OTHER, LocalDate.of(2026, 1, 1), BigDecimal.TEN, BigDecimal.ZERO, 5,
                false, BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(a, b);
    }
}
