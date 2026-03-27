package com.assetmind.api.dto;
import com.assetmind.core.domain.DepreciationMethod;
public record DepreciationRecommendationResponse(
        DepreciationMethod recommendedMethod,
        int suggestedUsefulLifeYears,
        double confidence,
        String rationale,
        String source
) {
}
