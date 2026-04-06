package com.assetmind.api.controller;

import com.assetmind.ai.model.DepreciationRecommendation;
import com.assetmind.ai.service.DepreciationRecommendationService;
import com.assetmind.api.dto.AiDepreciationRequest;
import com.assetmind.api.dto.AiDepreciationResponse;
import com.assetmind.api.dto.DepreciationRecommendationRequest;
import com.assetmind.api.dto.DepreciationRecommendationResponse;
import com.assetmind.core.domain.*;
import com.assetmind.core.service.DepreciationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DepreciationControllerTest {

    private DepreciationEngine depreciationEngine;
    private DepreciationRecommendationService recommendationService;
    private DepreciationController controller;

    @BeforeEach
    void setUp() {
        depreciationEngine = mock(DepreciationEngine.class);
        recommendationService = mock(DepreciationRecommendationService.class);
        controller = new DepreciationController(depreciationEngine, recommendationService);
    }

    @Test
    void runDelegatesToEngine() {
        ScheduleLine line = new ScheduleLine(1, new BigDecimal("10000"), new BigDecimal("2000"), new BigDecimal("8000"), "Year 1");
        when(depreciationEngine.calculateSchedule(any(DepreciationRequest.class))).thenReturn(List.of(line));

        DepreciationRequest request = new DepreciationRequest(
                "A-1", BookType.TAX, DepreciationMethod.STRAIGHT_LINE, AssetClass.COMPUTER_EQUIPMENT,
                LocalDate.of(2024, 1, 1), new BigDecimal("10000"), BigDecimal.ZERO, 5,
                false, BigDecimal.ZERO, BigDecimal.ZERO);

        List<ScheduleLine> result = controller.run(request);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).yearNumber());
    }

    @Test
    void recommendReturnsAiRecommendation() {
        DepreciationRecommendation rec = new DepreciationRecommendation(
                DepreciationMethod.MACRS_200DB_HY, 5, 0.85, "MACRS is optimal", "AI_GROQ");
        when(recommendationService.recommend("TX", "laptop", AssetClass.COMPUTER_EQUIPMENT, true, false))
                .thenReturn(rec);

        DepreciationRecommendationRequest request = new DepreciationRecommendationRequest(
                "TX", "laptop", AssetClass.COMPUTER_EQUIPMENT, true, false);

        DepreciationRecommendationResponse response = controller.recommend(request);

        assertEquals(DepreciationMethod.MACRS_200DB_HY, response.recommendedMethod());
        assertEquals(5, response.suggestedUsefulLifeYears());
        assertEquals(0.85, response.confidence());
        assertEquals("AI_GROQ", response.source());
    }

    @Test
    void aiRunCombinesRecommendationAndEngine() {
        DepreciationRecommendation rec = new DepreciationRecommendation(
                DepreciationMethod.MACRS_200DB_HY, 7, 0.9, "MACRS recommended", "RULE_FALLBACK");
        when(recommendationService.recommend("CA", "forklift", AssetClass.MACHINERY, false, false))
                .thenReturn(rec);

        ScheduleLine line = new ScheduleLine(1, new BigDecimal("35000"), new BigDecimal("5000"), new BigDecimal("30000"), "Year 1");
        when(depreciationEngine.calculateSchedule(any(DepreciationRequest.class))).thenReturn(List.of(line));

        AiDepreciationRequest request = new AiDepreciationRequest(
                "A-2", "CA", "forklift", AssetClass.MACHINERY, BookType.TAX,
                LocalDate.of(2024, 6, 1), new BigDecimal("35000"),
                null, false, null, null, false, false);

        AiDepreciationResponse response = controller.aiRun(request);

        assertEquals(DepreciationMethod.MACRS_200DB_HY, response.recommendedMethod());
        assertEquals(7, response.suggestedUsefulLifeYears());
        assertEquals("RULE_FALLBACK", response.aiSource());
        assertEquals(1, response.schedule().size());
    }

    @Test
    void aiRunUsesDefaultZeroForOptionalFields() {
        DepreciationRecommendation rec = new DepreciationRecommendation(
                DepreciationMethod.STRAIGHT_LINE, 10, 0.7, "SL for long life", "RULE_FALLBACK");
        when(recommendationService.recommend(anyString(), anyString(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(rec);
        when(depreciationEngine.calculateSchedule(any())).thenReturn(List.of());

        AiDepreciationRequest request = new AiDepreciationRequest(
                "A-3", "NY", "building", AssetClass.BUILDING, BookType.BOOK,
                LocalDate.of(2024, 1, 1), new BigDecimal("500000"),
                null, false, null, null, false, true);

        AiDepreciationResponse response = controller.aiRun(request);

        assertEquals(DepreciationMethod.STRAIGHT_LINE, response.recommendedMethod());
        assertEquals(10, response.suggestedUsefulLifeYears());
    }
}
