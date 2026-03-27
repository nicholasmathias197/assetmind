package com.assetmind.ai.model;
import com.assetmind.core.domain.DepreciationMethod;
public record DepreciationRecommendation(
        DepreciationMethod recommendedMethod,
        int suggestedUsefulLifeYears,
        double confidence,
        String rationale,
        String source
) {
}
