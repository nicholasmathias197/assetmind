package com.assetmind.ai.model;

import java.util.List;

public record BreakoutSuggestion(
        List<ComponentSuggestion> components,
        double confidence,
        String rationale,
        String source
) {
    public record ComponentSuggestion(
            String description,
            String assetClass,
            double costPercentage,
            int usefulLifeYears
    ) {
    }
}
