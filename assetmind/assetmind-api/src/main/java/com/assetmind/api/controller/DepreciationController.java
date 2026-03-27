package com.assetmind.api.controller;

import com.assetmind.ai.model.DepreciationRecommendation;
import com.assetmind.ai.service.DepreciationRecommendationService;
import com.assetmind.api.dto.AiDepreciationRequest;
import com.assetmind.api.dto.AiDepreciationResponse;
import com.assetmind.api.dto.DepreciationRecommendationRequest;
import com.assetmind.api.dto.DepreciationRecommendationResponse;
import com.assetmind.core.domain.DepreciationRequest;
import com.assetmind.core.domain.ScheduleLine;
import com.assetmind.core.service.DepreciationEngine;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/depreciation")
public class DepreciationController {

    private final DepreciationEngine depreciationEngine;
    private final DepreciationRecommendationService recommendationService;

    public DepreciationController(
            DepreciationEngine depreciationEngine,
            DepreciationRecommendationService recommendationService
    ) {
        this.depreciationEngine = depreciationEngine;
        this.recommendationService = recommendationService;
    }

    /**
     * Classic endpoint: caller supplies every depreciation parameter explicitly.
     */
    @PostMapping("/run")
    public List<ScheduleLine> run(@Valid @RequestBody DepreciationRequest request) {
        return depreciationEngine.calculateSchedule(request);
    }

    /**
     * AI-only endpoint: returns just the recommended method + useful life (no schedule).
     */
    @PostMapping("/recommend")
    public DepreciationRecommendationResponse recommend(@Valid @RequestBody DepreciationRecommendationRequest request) {
        DepreciationRecommendation rec = recommendationService.recommend(
                request.stateCode(),
                request.equipmentType(),
                request.assetClass(),
                request.immediateDeductionPreferred(),
                request.longHorizonAsset()
        );
        return new DepreciationRecommendationResponse(
                rec.recommendedMethod(),
                rec.suggestedUsefulLifeYears(),
                rec.confidence(),
                rec.rationale(),
                rec.source()
        );
    }

    /**
     * Combined AI + engine endpoint.
     * <p>
     * The AI (or rule-engine fallback) picks the depreciation method and useful-life
     * years based on {@code stateCode}, {@code equipmentType}, and cost preferences.
     * Those values are then fed directly into the depreciation engine, and the full
     * year-by-year schedule is returned alongside the AI rationale.
     * <p>
     * If {@code GROQ_API_KEY} is not set the rule-engine fallback is used transparently
     * and {@code aiSource} will read {@code "RULE_FALLBACK"}.
     */
    @PostMapping("/ai-run")
    public AiDepreciationResponse aiRun(@Valid @RequestBody AiDepreciationRequest request) {

        // Step 1 – Ask AI (or fallback) for method + useful life
        DepreciationRecommendation rec = recommendationService.recommend(
                request.stateCode(),
                request.equipmentType(),
                request.assetClass(),
                request.immediateDeductionPreferred(),
                request.longHorizonAsset()
        );

        // Step 2 – Build the engine request with AI-supplied method/life
        DepreciationRequest engineRequest = new DepreciationRequest(
                request.assetId(),
                request.bookType(),
                rec.recommendedMethod(),
                request.assetClass(),
                request.inServiceDate(),
                request.costBasis(),
                request.salvageValueOrZero(),
                rec.suggestedUsefulLifeYears(),
                request.section179Enabled(),
                request.section179AmountOrZero(),
                request.bonusDepreciationRateOrZero()
        );

        // Step 3 – Run the depreciation engine
        List<ScheduleLine> schedule = depreciationEngine.calculateSchedule(engineRequest);

        // Step 4 – Return recommendation metadata + full schedule
        return new AiDepreciationResponse(
                rec.recommendedMethod(),
                rec.suggestedUsefulLifeYears(),
                rec.confidence(),
                rec.rationale(),
                rec.source(),
                schedule
        );
    }
}

