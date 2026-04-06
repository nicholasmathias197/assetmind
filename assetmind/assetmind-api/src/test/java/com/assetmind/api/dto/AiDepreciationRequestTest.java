package com.assetmind.api.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AiDepreciationRequestTest {

    @Test
    void salvageValueOrZeroReturnsValueWhenPresent() {
        var request = new AiDepreciationRequest(
                "A-1", "TX", "laptop", null, null, null,
                new BigDecimal("1000"), new BigDecimal("200"),
                false, null, null, false, false);
        assertEquals(new BigDecimal("200"), request.salvageValueOrZero());
    }

    @Test
    void salvageValueOrZeroReturnsZeroWhenNull() {
        var request = new AiDepreciationRequest(
                "A-1", "TX", "laptop", null, null, null,
                new BigDecimal("1000"), null,
                false, null, null, false, false);
        assertEquals(BigDecimal.ZERO, request.salvageValueOrZero());
    }

    @Test
    void section179AmountOrZeroReturnsValueWhenPresent() {
        var request = new AiDepreciationRequest(
                "A-1", "TX", "laptop", null, null, null,
                new BigDecimal("1000"), null,
                true, new BigDecimal("500"), null, false, false);
        assertEquals(new BigDecimal("500"), request.section179AmountOrZero());
    }

    @Test
    void section179AmountOrZeroReturnsZeroWhenNull() {
        var request = new AiDepreciationRequest(
                "A-1", "TX", "laptop", null, null, null,
                new BigDecimal("1000"), null,
                false, null, null, false, false);
        assertEquals(BigDecimal.ZERO, request.section179AmountOrZero());
    }

    @Test
    void bonusDepreciationRateOrZeroReturnsValueWhenPresent() {
        var request = new AiDepreciationRequest(
                "A-1", "TX", "laptop", null, null, null,
                new BigDecimal("1000"), null,
                false, null, new BigDecimal("0.60"), false, false);
        assertEquals(new BigDecimal("0.60"), request.bonusDepreciationRateOrZero());
    }

    @Test
    void bonusDepreciationRateOrZeroReturnsZeroWhenNull() {
        var request = new AiDepreciationRequest(
                "A-1", "TX", "laptop", null, null, null,
                new BigDecimal("1000"), null,
                false, null, null, false, false);
        assertEquals(BigDecimal.ZERO, request.bonusDepreciationRateOrZero());
    }
}
