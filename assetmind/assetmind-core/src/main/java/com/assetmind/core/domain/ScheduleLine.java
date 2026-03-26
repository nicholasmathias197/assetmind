package com.assetmind.core.domain;

import java.math.BigDecimal;

public record ScheduleLine(
        int yearNumber,
        BigDecimal beginningBookValue,
        BigDecimal depreciationExpense,
        BigDecimal endingBookValue,
        String explanation
) {
}

