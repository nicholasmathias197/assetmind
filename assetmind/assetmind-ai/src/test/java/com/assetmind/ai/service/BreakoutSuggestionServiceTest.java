package com.assetmind.ai.service;

import com.assetmind.ai.model.BreakoutSuggestion;
import com.assetmind.ai.model.BreakoutSuggestion.ComponentSuggestion;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BreakoutSuggestionServiceTest {

    // -------------------------------------------------------------------------
    // No-AI keyword fallback tests (chatClient == null)
    // -------------------------------------------------------------------------

    @Test
    void keywordFallback_detectsPropertyBreakout() {
        BreakoutSuggestionService service = new BreakoutSuggestionService(null);

        BreakoutSuggestion result = service.suggest("Commercial office building purchase");

        assertEquals("RULE_FALLBACK", result.source());
        assertFalse(result.components().isEmpty());
        assertTrue(result.components().size() >= 5, "Property breakout should have multiple components");

        double totalPct = result.components().stream().mapToDouble(ComponentSuggestion::costPercentage).sum();
        assertEquals(100.0, totalPct, 0.01, "Component percentages should sum to 100");
    }

    @Test
    void keywordFallback_detectsOfficeBuildout() {
        BreakoutSuggestionService service = new BreakoutSuggestionService(null);

        BreakoutSuggestion result = service.suggest("Tenant improvement and office renovation");

        assertEquals("RULE_FALLBACK", result.source());
        assertFalse(result.components().isEmpty());
        assertTrue(result.components().stream()
                .anyMatch(c -> c.assetClass().contains("LEASEHOLD") || c.assetClass().contains("FURNITURE")));
    }

    @Test
    void keywordFallback_detectsDataCenter() {
        BreakoutSuggestionService service = new BreakoutSuggestionService(null);

        BreakoutSuggestion result = service.suggest("Data center equipment rack installation");

        assertEquals("RULE_FALLBACK", result.source());
        assertTrue(result.components().stream()
                .anyMatch(c -> c.assetClass().equals("COMPUTER_EQUIPMENT")));
    }

    @Test
    void keywordFallback_detectsVehicle() {
        BreakoutSuggestionService service = new BreakoutSuggestionService(null);

        BreakoutSuggestion result = service.suggest("Ford F-150 fleet truck purchase");

        assertEquals("RULE_FALLBACK", result.source());
        assertTrue(result.components().stream()
                .anyMatch(c -> c.assetClass().equals("VEHICLE")));
    }

    @Test
    void keywordFallback_returnsGenericBreakoutForUnknownText() {
        BreakoutSuggestionService service = new BreakoutSuggestionService(null);

        BreakoutSuggestion result = service.suggest("Miscellaneous items");

        assertEquals("RULE_FALLBACK", result.source());
        assertFalse(result.components().isEmpty());
    }

    @Test
    void keywordFallback_handlesNullInput() {
        BreakoutSuggestionService service = new BreakoutSuggestionService(null);

        BreakoutSuggestion result = service.suggest(null);

        assertEquals("RULE_FALLBACK", result.source());
        assertFalse(result.components().isEmpty());
    }

    // -------------------------------------------------------------------------
    // Mocked AI path tests (chatClient != null)
    // -------------------------------------------------------------------------

    @Test
    void aiPath_parsesJsonResponseCorrectly() throws Exception {
        String aiJson = """
                {
                  "components": [
                    {
                      "description": "Building structure",
                      "assetClass": "BUILDING",
                      "costPercentage": 60,
                      "usefulLifeYears": 39
                    },
                    {
                      "description": "Land",
                      "assetClass": "LAND",
                      "costPercentage": 25,
                      "usefulLifeYears": 0
                    },
                    {
                      "description": "HVAC system",
                      "assetClass": "BUILDING_IMPROVEMENT",
                      "costPercentage": 15,
                      "usefulLifeYears": 15
                    }
                  ],
                  "confidence": 0.92,
                  "rationale": "Commercial property breakout"
                }
                """;

        ChatClient chatClient = mockChatClientReturning(aiJson);
        BreakoutSuggestionService service = new BreakoutSuggestionService(chatClient);

        BreakoutSuggestion result = service.suggest("Office building at 123 Main St");

        assertEquals("AI_GROQ", result.source());
        assertEquals(3, result.components().size());
        assertEquals(0.92, result.confidence(), 0.001);
        assertEquals("Commercial property breakout", result.rationale());

        assertEquals("BUILDING", result.components().get(0).assetClass());
        assertEquals(60.0, result.components().get(0).costPercentage(), 0.01);
        assertEquals(39, result.components().get(0).usefulLifeYears());
    }

    @Test
    void aiPath_fallsBackToKeywordsOnException() {
        ChatClient chatClient = mock(ChatClient.class);
        when(chatClient.call(any(Prompt.class))).thenThrow(new RuntimeException("API error"));

        BreakoutSuggestionService service = new BreakoutSuggestionService(chatClient);

        BreakoutSuggestion result = service.suggest("Office building purchase");

        assertEquals("RULE_FALLBACK", result.source());
        assertFalse(result.components().isEmpty());
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private ChatClient mockChatClientReturning(String content) {
        ChatClient chatClient = mock(ChatClient.class);
        ChatResponse chatResponse = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage message = new AssistantMessage(content);

        when(chatClient.call(any(Prompt.class))).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(message);

        return chatClient;
    }
}
