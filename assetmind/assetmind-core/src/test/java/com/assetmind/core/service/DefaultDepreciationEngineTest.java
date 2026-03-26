package com.assetmind.core.service;

import com.assetmind.core.domain.AssetClass;
import com.assetmind.core.domain.BookType;
import com.assetmind.core.domain.DepreciationMethod;
import com.assetmind.core.domain.DepreciationRequest;
import com.assetmind.core.domain.ScheduleLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DefaultDepreciationEngineTest {

    private final DefaultDepreciationEngine engine = new DefaultDepreciationEngine();

    @Test
    void shouldGenerateStraightLineSchedule() {
        DepreciationRequest request = new DepreciationRequest(
                "asset-1",
                BookType.BOOK,
                DepreciationMethod.STRAIGHT_LINE,
                AssetClass.COMPUTER_EQUIPMENT,
                LocalDate.of(2026, 1, 1),
                new BigDecimal("10000.00"),
                new BigDecimal("1000.00"),
                3,
                false,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        List<ScheduleLine> lines = engine.calculateSchedule(request);

        assertEquals(3, lines.size());
        assertEquals(new BigDecimal("1000.00"), lines.get(2).endingBookValue());
        assertFalse(lines.isEmpty());
    }
}

