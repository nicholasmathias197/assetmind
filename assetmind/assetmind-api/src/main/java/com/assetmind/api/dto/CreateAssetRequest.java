package com.assetmind.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.assetmind.core.domain.AssetClass;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAssetRequest(
        @NotBlank String id,
        @NotBlank String description,
        @NotNull AssetClass assetClass,
        @NotNull @DecimalMin("0.0") BigDecimal costBasis,
        @NotNull LocalDate inServiceDate,
        @Min(0) int usefulLifeYears
) {
}

