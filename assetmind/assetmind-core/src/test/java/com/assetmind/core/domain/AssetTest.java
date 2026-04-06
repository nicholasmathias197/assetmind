package com.assetmind.core.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AssetTest {

    @Test
    void recordFieldAccess() {
        Asset asset = new Asset("A1", "Laptop", AssetClass.COMPUTER_EQUIPMENT,
                new BigDecimal("1500.00"), LocalDate.of(2026, 1, 15), 5, false);

        assertEquals("A1", asset.id());
        assertEquals("Laptop", asset.description());
        assertEquals(AssetClass.COMPUTER_EQUIPMENT, asset.assetClass());
        assertEquals(new BigDecimal("1500.00"), asset.costBasis());
        assertEquals(LocalDate.of(2026, 1, 15), asset.inServiceDate());
        assertEquals(5, asset.usefulLifeYears());
        assertFalse(asset.deleted());
    }

    @Test
    void deletedFlag() {
        Asset asset = new Asset("A2", "Desk", AssetClass.FURNITURE,
                new BigDecimal("800.00"), LocalDate.of(2026, 2, 1), 7, true);
        assertTrue(asset.deleted());
    }

    @Test
    void equalityBasedOnFields() {
        Asset a = new Asset("X", "item", AssetClass.OTHER, BigDecimal.TEN, LocalDate.of(2026, 1, 1), 10, false);
        Asset b = new Asset("X", "item", AssetClass.OTHER, BigDecimal.TEN, LocalDate.of(2026, 1, 1), 10, false);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
