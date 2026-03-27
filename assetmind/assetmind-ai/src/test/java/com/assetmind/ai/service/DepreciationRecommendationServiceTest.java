package com.assetmind.ai.service;

import com.assetmind.ai.model.DepreciationRecommendation;
import com.assetmind.core.domain.AssetClass;
import com.assetmind.core.domain.DepreciationMethod;
import com.assetmind.core.service.TaxStrategyAdvisor;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DepreciationRecommendationServiceTest {

    private final TaxStrategyAdvisor fallbackAdvisor = new TaxStrategyAdvisor();

    // -------------------------------------------------------------------------
    // No-AI fallback tests (chatClient == null)
    // -------------------------------------------------------------------------

    @Test
    void fallback_immediateDeductionPreferred_returnsMACRS() {
        DepreciationRecommendationService service =
                new DepreciationRecommendationService(null, fallbackAdvisor);

        DepreciationRecommendation result = service.recommend(
                "CA", "server rack", AssetClass.COMPUTER_EQUIPMENT, true, false);

        assertEquals(DepreciationMethod.MACRS_200DB_HY, result.recommendedMethod());
        assertEquals("RULE_FALLBACK", result.source());
        assertTrue(result.confidence() > 0);
    }

    @Test
    void fallback_longHorizonAsset_returnsADS() {
        DepreciationRecommendationService service =
                new DepreciationRecommendationService(null, fallbackAdvisor);

        DepreciationRecommendation result = service.recommend(
                "TX", "industrial boiler", AssetClass.BUILDING_IMPROVEMENT, false, true);

        assertEquals(DepreciationMethod.ADS_STRAIGHT_LINE, result.recommendedMethod());
        assertEquals("RULE_FALLBACK", result.source());
    }

    @Test
    void fallback_standard_returnsStraightLine() {
        DepreciationRecommendationService service =
                new DepreciationRecommendationService(null, fallbackAdvisor);

        DepreciationRecommendation result = service.recommend(
                "NY", "office desk", AssetClass.FURNITURE, false, false);

        assertEquals(DepreciationMethod.STRAIGHT_LINE, result.recommendedMethod());
        assertEquals("RULE_FALLBACK", result.source());
    }

    @Test
    void fallback_usefulLife_computerEquipmentIs5Years() {
        DepreciationRecommendationService service =
                new DepreciationRecommendationService(null, fallbackAdvisor);

        DepreciationRecommendation result = service.recommend(
                "WA", "workstation", AssetClass.COMPUTER_EQUIPMENT, false, false);

        assertEquals(5, result.suggestedUsefulLifeYears());
    }

    @Test
    void fallback_usefulLife_furnitureIs7Years() {
        DepreciationRecommendationService service =
                new DepreciationRecommendationService(null, fallbackAdvisor);

        DepreciationRecommendation result = service.recommend(
                "OR", "ergonomic chair", AssetClass.FURNITURE, false, false);

        assertEquals(7, result.suggestedUsefulLifeYears());
    }

    @Test
    void fallback_usefulLife_leaseholdIs15Years() {
        DepreciationRecommendationService service =
                new DepreciationRecommendationService(null, fallbackAdvisor);

        DepreciationRecommendation result = service.recommend(
                "IL", "tenant fit-out", AssetClass.LEASEHOLD_IMPROVEMENT, false, false);

        assertEquals(15, result.suggestedUsefulLifeYears());
    }

    @Test
    void fallback_usefulLife_buildingImprovementIs39Years() {
        DepreciationRecommendationService service =
                new DepreciationRecommendationService(null, fallbackAdvisor);

        DepreciationRecommendation result = service.recommend(
                "FL", "roof replacement", AssetClass.BUILDING_IMPROVEMENT, false, true);

        assertEquals(39, result.suggestedUsefulLifeYears());
    }

    @Test
    void fallback_usefulLife_vehicleIs5Years() {
        DepreciationRecommendationService service =
                new DepreciationRecommendationService(null, fallbackAdvisor);

        DepreciationRecommendation result = service.recommend(
                "MI", "delivery van", AssetClass.VEHICLE, false, false);

        assertEquals(5, result.suggestedUsefulLifeYears());
    }

    @Test
    void fallback_usefulLife_otherIs10Years() {
        DepreciationRecommendationService service =
                new DepreciationRecommendationService(null, fallbackAdvisor);

        DepreciationRecommendation result = service.recommend(
                "OH", "custom equipment", AssetClass.OTHER, false, false);

        assertEquals(10, result.suggestedUsefulLifeYears());
    }

    // -------------------------------------------------------------------------
    // Mocked AI path tests (chatClient != null)
    // -------------------------------------------------------------------------

    @Test
    void aiPath_parsesJsonResponseAndMapsFieldsCorrectly() throws Exception {
        String aiJson = """
                {
                  "recommendedMethod": "MACRS_200DB_HY",
                  "suggestedUsefulLifeYears": 5,
                  "confidence": 0.92,
                  "rationale": "MACRS 200DB is optimal for 5-year IT equipment"
                }
                """;

        ChatClient chatClient = mockChatClientReturning(aiJson);
        DepreciationRecommendationService service =
                new DepreciationRecommendationService(chatClient, fallbackAdvisor);

        DepreciationRecommendation result = service.recommend(
                "CA", "laptop", AssetClass.COMPUTER_EQUIPMENT, true, false);

        assertEquals(DepreciationMethod.MACRS_200DB_HY, result.recommendedMethod());
        assertEquals(5, result.suggestedUsefulLifeYears());
        assertEquals(0.92, result.confidence(), 0.001);
        assertEquals("MACRS 200DB is optimal for 5-year IT equipment", result.rationale());
        assertEquals("AI_GROQ", result.source());
    }

    @Test
    void aiPath_stripsMarkdownFencesBeforeParsing() throws Exception {
        String aiJsonWithFences = """
                ```json
                {
                  "recommendedMethod": "STRAIGHT_LINE",
                  "suggestedUsefulLifeYears": 7,
                  "confidence": 0.80,
                  "rationale": "Straight-line is appropriate for furniture"
                }
                ```
                """;

        ChatClient chatClient = mockChatClientReturning(aiJsonWithFences);
        DepreciationRecommendationService service =
                new DepreciationRecommendationService(chatClient, fallbackAdvisor);

        DepreciationRecommendation result = service.recommend(
                "NY", "office desk", AssetClass.FURNITURE, false, false);

        assertEquals(DepreciationMethod.STRAIGHT_LINE, result.recommendedMethod());
        assertEquals(7, result.suggestedUsefulLifeYears());
    }

    @Test
    void aiPath_exceptionCausesFallbackToRuleEngine() throws Exception {
        ChatClient chatClient = mock(ChatClient.class);
        when(chatClient.call(any(Prompt.class))).thenThrow(new RuntimeException("network error"));

        DepreciationRecommendationService service =
                new DepreciationRecommendationService(chatClient, fallbackAdvisor);

        DepreciationRecommendation result = service.recommend(
                "TX", "server", AssetClass.COMPUTER_EQUIPMENT, true, false);

        // Should fall back to RULE_FALLBACK with MACRS (immediateDeductionPreferred=true)
        assertEquals(DepreciationMethod.MACRS_200DB_HY, result.recommendedMethod());
        assertEquals("RULE_FALLBACK", result.source());
    }

    @Test
    void fallback_nullStateCode_doesNotThrow() {
        DepreciationRecommendationService service =
                new DepreciationRecommendationService(null, fallbackAdvisor);

        DepreciationRecommendation result = service.recommend(
                null, "laptop", AssetClass.COMPUTER_EQUIPMENT, false, false);

        assertEquals("RULE_FALLBACK", result.source());
        assertTrue(result.rationale().contains("UNKNOWN"));
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private ChatClient mockChatClientReturning(String content) {
        ChatClient chatClient       = mock(ChatClient.class);
        ChatResponse chatResponse   = mock(ChatResponse.class);
        Generation generation       = mock(Generation.class);
        AssistantMessage message    = mock(AssistantMessage.class);

        when(chatClient.call(any(Prompt.class))).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(message);
        when(message.getContent()).thenReturn(content);

        return chatClient;
    }
}

