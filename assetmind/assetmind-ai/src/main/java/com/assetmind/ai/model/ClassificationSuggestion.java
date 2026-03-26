package com.assetmind.ai.model;

import com.assetmind.core.domain.AssetClass;

public record ClassificationSuggestion(
        AssetClass assetClass,
        String glCode,
        int usefulLifeYears,
        double confidence,
        String rationale
) {
}

