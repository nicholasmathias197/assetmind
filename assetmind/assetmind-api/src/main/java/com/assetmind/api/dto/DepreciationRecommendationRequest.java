package com.assetmind.api.dto;

import com.assetmind.core.domain.AssetClass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DepreciationRecommendationRequest(
        @NotBlank String stateCode,
        @NotBlank String equipmentType,
        @NotNull AssetClass assetClass,
        boolean immediateDeductionPreferred,
        boolean longHorizonAsset
) {
}

