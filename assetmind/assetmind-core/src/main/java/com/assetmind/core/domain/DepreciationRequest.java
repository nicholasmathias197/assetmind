package com.assetmind.core.domain;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DepreciationRequest(
        @NotNull String assetId,
        @NotNull BookType bookType,
        @NotNull DepreciationMethod method,
        @NotNull AssetClass assetClass,
        @NotNull LocalDate inServiceDate,
        @NotNull @DecimalMin("0.0") BigDecimal costBasis,
        @NotNull @DecimalMin("0.0") BigDecimal salvageValue,
        int usefulLifeYears,
        boolean section179Enabled,
        @DecimalMin("0.0") BigDecimal section179Amount,
        @DecimalMin("0.0") BigDecimal bonusDepreciationRate
) {
}

