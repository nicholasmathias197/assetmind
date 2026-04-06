package com.assetmind.core.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnumTests {

    @Test
    void assetClassValues() {
        AssetClass[] values = AssetClass.values();
        assertEquals(9, values.length);
        assertEquals(AssetClass.COMPUTER_EQUIPMENT, AssetClass.valueOf("COMPUTER_EQUIPMENT"));
        assertEquals(AssetClass.FURNITURE, AssetClass.valueOf("FURNITURE"));
        assertEquals(AssetClass.LEASEHOLD_IMPROVEMENT, AssetClass.valueOf("LEASEHOLD_IMPROVEMENT"));
        assertEquals(AssetClass.BUILDING_IMPROVEMENT, AssetClass.valueOf("BUILDING_IMPROVEMENT"));
        assertEquals(AssetClass.VEHICLE, AssetClass.valueOf("VEHICLE"));
        assertEquals(AssetClass.LAND, AssetClass.valueOf("LAND"));
        assertEquals(AssetClass.BUILDING, AssetClass.valueOf("BUILDING"));
        assertEquals(AssetClass.MACHINERY, AssetClass.valueOf("MACHINERY"));
        assertEquals(AssetClass.OTHER, AssetClass.valueOf("OTHER"));
    }

    @Test
    void bookTypeValues() {
        BookType[] values = BookType.values();
        assertEquals(3, values.length);
        assertEquals(BookType.BOOK, BookType.valueOf("BOOK"));
        assertEquals(BookType.TAX, BookType.valueOf("TAX"));
        assertEquals(BookType.STATE, BookType.valueOf("STATE"));
    }

    @Test
    void depreciationMethodValues() {
        DepreciationMethod[] values = DepreciationMethod.values();
        assertEquals(3, values.length);
        assertEquals(DepreciationMethod.STRAIGHT_LINE, DepreciationMethod.valueOf("STRAIGHT_LINE"));
        assertEquals(DepreciationMethod.MACRS_200DB_HY, DepreciationMethod.valueOf("MACRS_200DB_HY"));
        assertEquals(DepreciationMethod.ADS_STRAIGHT_LINE, DepreciationMethod.valueOf("ADS_STRAIGHT_LINE"));
    }

    @Test
    void invalidAssetClassThrows() {
        assertThrows(IllegalArgumentException.class, () -> AssetClass.valueOf("INVALID"));
    }

    @Test
    void invalidBookTypeThrows() {
        assertThrows(IllegalArgumentException.class, () -> BookType.valueOf("INVALID"));
    }

    @Test
    void invalidDepreciationMethodThrows() {
        assertThrows(IllegalArgumentException.class, () -> DepreciationMethod.valueOf("INVALID"));
    }
}
