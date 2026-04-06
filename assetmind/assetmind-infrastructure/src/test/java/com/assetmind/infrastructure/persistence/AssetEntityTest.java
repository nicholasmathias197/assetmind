package com.assetmind.infrastructure.persistence;

import com.assetmind.core.domain.AssetClass;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AssetEntityTest {

    @Test
    void defaultDeletedIsFalse() {
        AssetEntity entity = new AssetEntity();
        assertFalse(entity.isDeleted());
    }

    @Test
    void gettersAndSetters() {
        AssetEntity entity = new AssetEntity();
        entity.setId("A-001");
        entity.setDescription("Dell laptop");
        entity.setAssetClass(AssetClass.COMPUTER_EQUIPMENT);
        entity.setCostBasis(new BigDecimal("1200.50"));
        entity.setInServiceDate(LocalDate.of(2024, 1, 15));
        entity.setUsefulLifeYears(5);
        entity.setDeleted(true);

        assertEquals("A-001", entity.getId());
        assertEquals("Dell laptop", entity.getDescription());
        assertEquals(AssetClass.COMPUTER_EQUIPMENT, entity.getAssetClass());
        assertEquals(new BigDecimal("1200.50"), entity.getCostBasis());
        assertEquals(LocalDate.of(2024, 1, 15), entity.getInServiceDate());
        assertEquals(5, entity.getUsefulLifeYears());
        assertTrue(entity.isDeleted());
    }
}
