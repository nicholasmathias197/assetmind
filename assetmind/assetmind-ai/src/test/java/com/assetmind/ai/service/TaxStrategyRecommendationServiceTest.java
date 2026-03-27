package com.assetmind.ai.service;

import com.assetmind.ai.model.TaxStrategyRecommendation;
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

class TaxStrategyRecommendationServiceTest {

    private final TaxStrategyAdvisor fallbackAdvisor = new TaxStrategyAdvisor();

    // -------------------------------------------------------------------------
    // No-AI fallback tests (chatClient == null)
    // -------------------------------------------------------------------------

    @Test
    void fallback_immediateDeductionPreferred_returnsMACRS() {
        TaxStrategyRecommendationService service =
                new TaxStrategyRecommendationService(null, fallbackAdvisor);

        TaxStrategyRecommendation result = service.recommend(
                "CA", "server", true, false);

        assertEquals(DepreciationMethod.MACRS_200DB_HY, result.recommendedMethod());
        assertEquals("RULE_FALLBACK", result.source());
        assertTrue(result.confidence() > 0);
    }

    @Test
    void fallback_longHorizonAsset_returnsADS() {
        TaxStrategyRecommendationService service =
                new TaxStrategyRecommendationService(null, fallbackAdvisor);

        TaxStrategyRecommendation result = service.recommend(
                "TX", "industrial boiler", false, true);

        assertEquals(DepreciationMethod.ADS_STRAIGHT_LINE, result.recommendedMethod());
        assertEquals("RULE_FALLBACK", result.source());
    }

    @Test
    void fallback_standardAsset_returnsStraightLine() {
        TaxStrategyRecommendationService service =
                new TaxStrategyRecommendationService(null, fallbackAdvisor);

        TaxStrategyRecommendation result = service.recommend(
                "NY", "office desk", false, false);

        assertEquals(DepreciationMethod.STRAIGHT_LINE, result.recommendedMethod());
        assertEquals("RULE_FALLBACK", result.source());
    }

    @Test
    void fallback_nullStateCode_doesNotThrow() {
        TaxStrategyRecommendationService service =
                new TaxStrategyRecommendationService(null, fallbackAdvisor);

        TaxStrategyRecommendation result = service.recommend(
                null, "laptop", false, false);

        assertEquals("RULE_FALLBACK", result.source());
        assertTrue(result.rationale().contains("UNKNOWN"));
    }

    @Test
    void fallback_rationaleMentionsEquipmentTypeAndState() {
        TaxStrategyRecommendationService service =
                new TaxStrategyRecommendationService(null, fallbackAdvisor);

        TaxStrategyRecommendation result = service.recommend(
                "fl", "HVAC unit", false, false);

        // stateCode should be normalised to uppercase in rationale
        assertTrue(result.rationale().contains("FL"));
        assertTrue(result.rationale().toLowerCase().contains("hvac unit"));
    }

    // -------------------------------------------------------------------------
    // Mocked AI path tests (chatClient != null)
    // -------------------------------------------------------------------------

    @Test
    void aiPath_parsesJsonResponseAndMapsFieldsCorrectly() throws Exception {
        String aiJson = """
                {
                  "recommendedMethod": "MACRS_200DB_HY",
                  "confidence": 0.91,
                  "rationale": "MACRS accelerated depreciation maximises early deductions in California"
                }
                """;

        ChatClient chatClient = mockChatClientReturning(aiJson);
        TaxStrategyRecommendationService service =
                new TaxStrategyRecommendationService(chatClient, fallbackAdvisor);

        TaxStrategyRecommendation result = service.recommend(
                "CA", "server", true, false);

        assertEquals(DepreciationMethod.MACRS_200DB_HY, result.recommendedMethod());
        assertEquals(0.91, result.confidence(), 0.001);
        assertEquals("MACRS accelerated depreciation maximises early deductions in California",
                result.rationale());
        assertEquals("AI_GROQ", result.source());
    }

    @Test
    void aiPath_parsesADSResponseCorrectly() throws Exception {
        String aiJson = """
                {
                  "recommendedMethod": "ADS_STRAIGHT_LINE",
                  "confidence": 0.85,
                  "rationale": "ADS required for listed property used outside the US"
                }
                """;

        ChatClient chatClient = mockChatClientReturning(aiJson);
        TaxStrategyRecommendationService service =
                new TaxStrategyRecommendationService(chatClient, fallbackAdvisor);

        TaxStrategyRecommendation result = service.recommend(
                "TX", "overseas equipment", false, true);

        assertEquals(DepreciationMethod.ADS_STRAIGHT_LINE, result.recommendedMethod());
        assertEquals("AI_GROQ", result.source());
    }

    @Test
    void aiPath_stripsMarkdownFencesBeforeParsing() throws Exception {
        String aiJsonWithFences = """
                ```json
                {
                  "recommendedMethod": "STRAIGHT_LINE",
                  "confidence": 0.78,
                  "rationale": "Straight-line suits long-lived structural assets"
                }
                ```
                """;

        ChatClient chatClient = mockChatClientReturning(aiJsonWithFences);
        TaxStrategyRecommendationService service =
                new TaxStrategyRecommendationService(chatClient, fallbackAdvisor);

        TaxStrategyRecommendation result = service.recommend(
                "NY", "building", false, false);

        assertEquals(DepreciationMethod.STRAIGHT_LINE, result.recommendedMethod());
    }

    @Test
    void aiPath_exceptionCausesFallbackToRuleEngine() throws Exception {
        ChatClient chatClient = mock(ChatClient.class);
        when(chatClient.call(any(Prompt.class))).thenThrow(new RuntimeException("timeout"));

        TaxStrategyRecommendationService service =
                new TaxStrategyRecommendationService(chatClient, fallbackAdvisor);

        TaxStrategyRecommendation result = service.recommend(
                "OR", "machine", true, false);

        assertEquals(DepreciationMethod.MACRS_200DB_HY, result.recommendedMethod());
        assertEquals("RULE_FALLBACK", result.source());
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

