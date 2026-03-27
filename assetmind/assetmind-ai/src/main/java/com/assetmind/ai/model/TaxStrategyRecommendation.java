package com.assetmind.ai.model;

import com.assetmind.core.domain.DepreciationMethod;

public record TaxStrategyRecommendation(
        DepreciationMethod recommendedMethod,
        double confidence,
        String rationale,
        String source
) {
}

