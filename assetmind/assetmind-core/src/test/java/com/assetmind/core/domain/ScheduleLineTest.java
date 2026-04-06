package com.assetmind.core.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleLineTest {

    @Test
    void recordFieldAccess() {
        ScheduleLine line = new ScheduleLine(1, new BigDecimal("10000.00"),
                new BigDecimal("2000.00"), new BigDecimal("8000.00"), "Test explanation");

        assertEquals(1, line.yearNumber());
        assertEquals(new BigDecimal("10000.00"), line.beginningBookValue());
        assertEquals(new BigDecimal("2000.00"), line.depreciationExpense());
        assertEquals(new BigDecimal("8000.00"), line.endingBookValue());
        assertEquals("Test explanation", line.explanation());
    }

    @Test
    void equality() {
        ScheduleLine a = new ScheduleLine(1, BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("9"), "exp");
        ScheduleLine b = new ScheduleLine(1, BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("9"), "exp");
        assertEquals(a, b);
    }
}
