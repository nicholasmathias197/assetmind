package com.assetmind.api.dto;

public record TaxStrategyRequest(
        boolean immediateDeductionPreferred,
        boolean longHorizonAsset
) {
}

