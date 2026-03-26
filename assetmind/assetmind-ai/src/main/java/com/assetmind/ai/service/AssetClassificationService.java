package com.assetmind.ai.service;

import com.assetmind.ai.model.ClassificationSuggestion;
import com.assetmind.core.domain.AssetClass;
import org.springframework.stereotype.Service;

@Service
public class AssetClassificationService {

    public ClassificationSuggestion suggestFromInvoiceText(String documentText) {
        String normalized = documentText == null ? "" : documentText.toLowerCase();

        if (normalized.contains("laptop") || normalized.contains("server") || normalized.contains("workstation")) {
            return new ClassificationSuggestion(AssetClass.COMPUTER_EQUIPMENT, "1610", 5, 0.91d,
                    "Detected IT equipment keywords from invoice text");
        }
        if (normalized.contains("desk") || normalized.contains("chair") || normalized.contains("furniture")) {
            return new ClassificationSuggestion(AssetClass.FURNITURE, "1620", 7, 0.87d,
                    "Detected office furniture keywords from invoice text");
        }
        if (normalized.contains("leasehold") || normalized.contains("tenant improvement")) {
            return new ClassificationSuggestion(AssetClass.LEASEHOLD_IMPROVEMENT, "1710", 15, 0.82d,
                    "Detected leasehold improvement terms from invoice text");
        }

        return new ClassificationSuggestion(AssetClass.OTHER, "1699", 10, 0.52d,
                "Fallback classification due to low confidence");
    }
}

