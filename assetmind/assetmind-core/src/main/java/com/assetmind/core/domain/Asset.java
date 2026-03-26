package com.assetmind.core.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Asset(
        String id,
        String description,
        AssetClass assetClass,
        BigDecimal costBasis,
        LocalDate inServiceDate,
        int usefulLifeYears,
        boolean deleted
) {
}

