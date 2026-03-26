package com.assetmind.api.dto;

import com.assetmind.core.domain.AssetClass;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AssetResponse(
        String id,
        String description,
        AssetClass assetClass,
        BigDecimal costBasis,
        LocalDate inServiceDate,
        int usefulLifeYears
) {
}

