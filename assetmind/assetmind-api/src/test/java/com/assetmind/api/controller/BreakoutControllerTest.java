package com.assetmind.api.controller;

import com.assetmind.ai.model.BreakoutSuggestion;
import com.assetmind.ai.service.BreakoutSuggestionService;
import com.assetmind.api.dto.BreakoutSuggestRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BreakoutControllerTest {

    @Test
    void suggestDelegatesToService() {
        BreakoutSuggestionService service = mock(BreakoutSuggestionService.class);
        BreakoutSuggestion suggestion = new BreakoutSuggestion(
                List.of(new BreakoutSuggestion.ComponentSuggestion("Structure", "BUILDING", 0.6, 39)),
                0.8, "Property breakout", "RULE_FALLBACK");
        when(service.suggest("commercial building purchase")).thenReturn(suggestion);

        BreakoutController controller = new BreakoutController(service);
        BreakoutSuggestion result = controller.suggest(new BreakoutSuggestRequest("commercial building purchase"));

        assertEquals(1, result.components().size());
        assertEquals(0.8, result.confidence());
        assertEquals("RULE_FALLBACK", result.source());
    }
}
