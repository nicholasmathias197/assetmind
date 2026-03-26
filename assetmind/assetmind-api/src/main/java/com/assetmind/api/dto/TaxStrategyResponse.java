package com.assetmind.api.dto;

import com.assetmind.core.domain.DepreciationMethod;

public record TaxStrategyResponse(
        DepreciationMethod recommendedMethod
) {
}

