package com.assetmind.api.controller;

import com.assetmind.ai.model.TaxStrategyRecommendation;
import com.assetmind.ai.service.TaxStrategyRecommendationService;
import com.assetmind.api.dto.TaxStrategyRequest;
import com.assetmind.api.dto.TaxStrategyResponse;
import com.assetmind.core.domain.DepreciationMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaxStrategyControllerTest {

    @Test
    void recommendDelegatesToService() {
        TaxStrategyRecommendationService service = mock(TaxStrategyRecommendationService.class);
        TaxStrategyRecommendation rec = new TaxStrategyRecommendation(
                DepreciationMethod.MACRS_200DB_HY, 0.9, "Accelerated best for tax", "RULE_FALLBACK");
        when(service.recommend("TX", "laptop", true, false)).thenReturn(rec);

        TaxStrategyController controller = new TaxStrategyController(service);
        TaxStrategyResponse response = controller.recommend(
                new TaxStrategyRequest("TX", "laptop", true, false));

        assertEquals(DepreciationMethod.MACRS_200DB_HY, response.recommendedMethod());
        assertEquals(0.9, response.confidence());
        assertEquals("Accelerated best for tax", response.rationale());
        assertEquals("RULE_FALLBACK", response.source());
    }

    @Test
    void recommendWithLongHorizon() {
        TaxStrategyRecommendationService service = mock(TaxStrategyRecommendationService.class);
        TaxStrategyRecommendation rec = new TaxStrategyRecommendation(
                DepreciationMethod.ADS_STRAIGHT_LINE, 0.85, "ADS for long-lived asset", "AI_GROQ");
        when(service.recommend("CA", "building", false, true)).thenReturn(rec);

        TaxStrategyController controller = new TaxStrategyController(service);
        TaxStrategyResponse response = controller.recommend(
                new TaxStrategyRequest("CA", "building", false, true));

        assertEquals(DepreciationMethod.ADS_STRAIGHT_LINE, response.recommendedMethod());
        assertEquals("AI_GROQ", response.source());
    }
}
