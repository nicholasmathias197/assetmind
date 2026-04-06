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

import static org.junit.jupiter.api.Assertions.*;

class DefaultDepreciationEngineTest {

    private final DefaultDepreciationEngine engine = new DefaultDepreciationEngine();

    // ── Straight-line tests ─────────────────────────────────────────────

    @Test
    void shouldGenerateStraightLineSchedule() {
        DepreciationRequest request = new DepreciationRequest(
                "asset-1", BookType.BOOK, DepreciationMethod.STRAIGHT_LINE,
                AssetClass.COMPUTER_EQUIPMENT, LocalDate.of(2026, 1, 1),
                new BigDecimal("10000.00"), new BigDecimal("1000.00"), 3,
                false, BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<ScheduleLine> lines = engine.calculateSchedule(request);

        assertEquals(3, lines.size());
        assertEquals(new BigDecimal("10000.00"), lines.get(0).beginningBookValue());
        assertEquals(new BigDecimal("1000.00"), lines.get(2).endingBookValue());
    }

    @Test
    void straightLine_zeroSalvage() {
        DepreciationRequest request = new DepreciationRequest(
                "a2", BookType.BOOK, DepreciationMethod.STRAIGHT_LINE,
                AssetClass.FURNITURE, LocalDate.of(2026, 1, 1),
                new BigDecimal("7000.00"), BigDecimal.ZERO, 7,
                false, BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<ScheduleLine> lines = engine.calculateSchedule(request);

        assertEquals(7, lines.size());
        assertEquals(0, lines.get(6).endingBookValue().compareTo(BigDecimal.ZERO));
    }

    @Test
    void straightLine_oneYear() {
        DepreciationRequest request = new DepreciationRequest(
                "a3", BookType.BOOK, DepreciationMethod.STRAIGHT_LINE,
                AssetClass.OTHER, LocalDate.of(2026, 6, 1),
                new BigDecimal("5000.00"), new BigDecimal("500.00"), 1,
                false, BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<ScheduleLine> lines = engine.calculateSchedule(request);

        assertEquals(1, lines.size());
        assertEquals(new BigDecimal("4500.00"), lines.get(0).depreciationExpense());
        assertEquals(new BigDecimal("500.00"), lines.get(0).endingBookValue());
    }

    @Test
    void straightLine_zeroUsefulLifeDefaultsToOne() {
        DepreciationRequest request = new DepreciationRequest(
                "a4", BookType.BOOK, DepreciationMethod.STRAIGHT_LINE,
                AssetClass.OTHER, LocalDate.of(2026, 1, 1),
                new BigDecimal("3000.00"), BigDecimal.ZERO, 0,
                false, BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<ScheduleLine> lines = engine.calculateSchedule(request);

        assertEquals(1, lines.size());
        assertEquals(0, lines.get(0).endingBookValue().compareTo(BigDecimal.ZERO));
    }

    // ── ADS straight-line tests ─────────────────────────────────────────

    @Test
    void adsMethodProducesStraightLineSchedule() {
        DepreciationRequest request = new DepreciationRequest(
                "ads-1", BookType.TAX, DepreciationMethod.ADS_STRAIGHT_LINE,
                AssetClass.BUILDING, LocalDate.of(2026, 1, 1),
                new BigDecimal("390000.00"), BigDecimal.ZERO, 39,
                false, BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<ScheduleLine> lines = engine.calculateSchedule(request);

        assertEquals(39, lines.size());
        assertEquals(new BigDecimal("10000.00"), lines.get(0).depreciationExpense());
        assertEquals(0, lines.get(38).endingBookValue().compareTo(BigDecimal.ZERO));
    }

    // ── MACRS half-year tests ───────────────────────────────────────────

    @Test
    void macrsHalfYear_5yearAsset() {
        DepreciationRequest request = new DepreciationRequest(
                "m1", BookType.TAX, DepreciationMethod.MACRS_200DB_HY,
                AssetClass.COMPUTER_EQUIPMENT, LocalDate.of(2026, 1, 1),
                new BigDecimal("10000.00"), BigDecimal.ZERO, 5,
                false, BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<ScheduleLine> lines = engine.calculateSchedule(request);

        assertFalse(lines.isEmpty());
        // MACRS half-year first-year rate = 200%/5 / 2 = 20%
        assertEquals(new BigDecimal("2000.00"), lines.get(0).depreciationExpense());
        // Final book value should be zero
        assertEquals(0, lines.get(lines.size() - 1).endingBookValue().compareTo(BigDecimal.ZERO));
    }

    @Test
    void macrsHalfYear_3yearAsset() {
        DepreciationRequest request = new DepreciationRequest(
                "m2", BookType.TAX, DepreciationMethod.MACRS_200DB_HY,
                AssetClass.OTHER, LocalDate.of(2026, 1, 1),
                new BigDecimal("9000.00"), BigDecimal.ZERO, 3,
                false, BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<ScheduleLine> lines = engine.calculateSchedule(request);

        assertFalse(lines.isEmpty());
        // First year half-rate: 2/3 / 2 = 1/3 => 3000
        assertEquals(new BigDecimal("3000.00"), lines.get(0).depreciationExpense());
        assertEquals(0, lines.get(lines.size() - 1).endingBookValue().compareTo(BigDecimal.ZERO));
    }

    @Test
    void macrsHalfYear_1yearAsset() {
        DepreciationRequest request = new DepreciationRequest(
                "m3", BookType.TAX, DepreciationMethod.MACRS_200DB_HY,
                AssetClass.OTHER, LocalDate.of(2026, 1, 1),
                new BigDecimal("4000.00"), BigDecimal.ZERO, 1,
                false, BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<ScheduleLine> lines = engine.calculateSchedule(request);

        assertFalse(lines.isEmpty());
        assertTrue(lines.size() <= 2);
        assertEquals(0, lines.get(lines.size() - 1).endingBookValue().compareTo(BigDecimal.ZERO));
    }

    // ── Section 179 tests ───────────────────────────────────────────────

    @Test
    void section179ReducesBasis() {
        DepreciationRequest request = new DepreciationRequest(
                "s1", BookType.TAX, DepreciationMethod.STRAIGHT_LINE,
                AssetClass.COMPUTER_EQUIPMENT, LocalDate.of(2026, 1, 1),
                new BigDecimal("10000.00"), BigDecimal.ZERO, 5,
                true, new BigDecimal("4000.00"), BigDecimal.ZERO
        );

        List<ScheduleLine> lines = engine.calculateSchedule(request);

        // Basis reduced by 4000 → 6000 depreciated over 5 years
        assertEquals(new BigDecimal("6000.00"), lines.get(0).beginningBookValue());
        assertEquals(5, lines.size());
    }

    @Test
    void section179CannotExceedBasis() {
        DepreciationRequest request = new DepreciationRequest(
                "s2", BookType.TAX, DepreciationMethod.STRAIGHT_LINE,
                AssetClass.COMPUTER_EQUIPMENT, LocalDate.of(2026, 1, 1),
                new BigDecimal("1000.00"), BigDecimal.ZERO, 5,
                true, new BigDecimal("5000.00"), BigDecimal.ZERO
        );

        List<ScheduleLine> lines = engine.calculateSchedule(request);

        // Section 179 capped at costBasis, remainder is 0
        assertEquals(0, lines.get(0).beginningBookValue().compareTo(BigDecimal.ZERO));
    }

    // ── Bonus depreciation tests ────────────────────────────────────────

    @Test
    void bonusDepreciationReducesBasis() {
        DepreciationRequest request = new DepreciationRequest(
                "b1", BookType.TAX, DepreciationMethod.STRAIGHT_LINE,
                AssetClass.MACHINERY, LocalDate.of(2026, 1, 1),
                new BigDecimal("10000.00"), BigDecimal.ZERO, 5,
                false, BigDecimal.ZERO, new BigDecimal("0.60")
        );

        List<ScheduleLine> lines = engine.calculateSchedule(request);

        // 60% bonus → 6000 removed, 4000 remaining
        assertEquals(new BigDecimal("4000.00"), lines.get(0).beginningBookValue());
    }

    @Test
    void section179AndBonusCombined() {
        DepreciationRequest request = new DepreciationRequest(
                "sb1", BookType.TAX, DepreciationMethod.STRAIGHT_LINE,
                AssetClass.COMPUTER_EQUIPMENT, LocalDate.of(2026, 1, 1),
                new BigDecimal("10000.00"), BigDecimal.ZERO, 5,
                true, new BigDecimal("2000.00"), new BigDecimal("0.50")
        );

        List<ScheduleLine> lines = engine.calculateSchedule(request);

        // 10000 - 2000(179) = 8000, then 50% bonus = 4000 remaining
        assertEquals(new BigDecimal("4000.00"), lines.get(0).beginningBookValue());
    }

    // ── Schedule line structure tests ───────────────────────────────────

    @Test
    void scheduleLineContainsExplanation() {
        DepreciationRequest request = new DepreciationRequest(
                "e1", BookType.BOOK, DepreciationMethod.STRAIGHT_LINE,
                AssetClass.FURNITURE, LocalDate.of(2026, 1, 1),
                new BigDecimal("7000.00"), BigDecimal.ZERO, 7,
                false, BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<ScheduleLine> lines = engine.calculateSchedule(request);

        assertNotNull(lines.get(0).explanation());
        assertTrue(lines.get(0).explanation().contains("Straight-line"));
    }

    @Test
    void macrsScheduleLineContainsExplanation() {
        DepreciationRequest request = new DepreciationRequest(
                "e2", BookType.TAX, DepreciationMethod.MACRS_200DB_HY,
                AssetClass.VEHICLE, LocalDate.of(2026, 1, 1),
                new BigDecimal("30000.00"), BigDecimal.ZERO, 5,
                false, BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<ScheduleLine> lines = engine.calculateSchedule(request);

        assertNotNull(lines.get(0).explanation());
        assertTrue(lines.get(0).explanation().contains("MACRS"));
    }

    @Test
    void yearNumbersAreSequential() {
        DepreciationRequest request = new DepreciationRequest(
                "seq1", BookType.BOOK, DepreciationMethod.STRAIGHT_LINE,
                AssetClass.MACHINERY, LocalDate.of(2026, 1, 1),
                new BigDecimal("14000.00"), BigDecimal.ZERO, 7,
                false, BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<ScheduleLine> lines = engine.calculateSchedule(request);

        for (int i = 0; i < lines.size(); i++) {
            assertEquals(i + 1, lines.get(i).yearNumber());
        }
    }

    @Test
    void endingBookValueMatchesNextBeginningBookValue() {
        DepreciationRequest request = new DepreciationRequest(
                "chain1", BookType.BOOK, DepreciationMethod.STRAIGHT_LINE,
                AssetClass.BUILDING, LocalDate.of(2026, 1, 1),
                new BigDecimal("39000.00"), BigDecimal.ZERO, 3,
                false, BigDecimal.ZERO, BigDecimal.ZERO
        );

        List<ScheduleLine> lines = engine.calculateSchedule(request);

        for (int i = 0; i < lines.size() - 1; i++) {
            assertEquals(lines.get(i).endingBookValue(), lines.get(i + 1).beginningBookValue());
        }
    }
}

