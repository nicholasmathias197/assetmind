package com.assetmind.api.controller;

import com.assetmind.ai.model.TaxStrategyRecommendation;
import com.assetmind.ai.service.TaxStrategyRecommendationService;
import com.assetmind.api.dto.TaxStrategyRequest;
import com.assetmind.api.dto.TaxStrategyResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tax-strategy")
public class TaxStrategyController {

    private final TaxStrategyRecommendationService recommendationService;

    public TaxStrategyController(TaxStrategyRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping("/recommend")
    public TaxStrategyResponse recommend(@Valid @RequestBody TaxStrategyRequest request) {
        TaxStrategyRecommendation recommendation = recommendationService.recommend(
                request.stateCode(),
                request.equipmentType(),
                request.immediateDeductionPreferred(),
                request.longHorizonAsset()
        );
        return new TaxStrategyResponse(
                recommendation.recommendedMethod(),
                recommendation.confidence(),
                recommendation.rationale(),
                recommendation.source()
        );
    }
}

