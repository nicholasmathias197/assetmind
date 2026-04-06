package com.assetmind.api.controller;

import com.assetmind.ai.model.ClassificationSuggestion;
import com.assetmind.ai.service.AssetClassificationService;
import com.assetmind.api.dto.ClassificationRequest;
import com.assetmind.core.domain.AssetClass;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClassificationControllerTest {

    @Test
    void suggestDelegatesToService() {
        AssetClassificationService service = mock(AssetClassificationService.class);
        ClassificationSuggestion suggestion = new ClassificationSuggestion(
                AssetClass.COMPUTER_EQUIPMENT, "GL-5100", 5, 0.93, "Laptop classification");
        when(service.suggestFromInvoiceText("Dell XPS 15")).thenReturn(suggestion);

        ClassificationController controller = new ClassificationController(service);
        ClassificationSuggestion result = controller.suggest(new ClassificationRequest("Dell XPS 15"));

        assertEquals(AssetClass.COMPUTER_EQUIPMENT, result.assetClass());
        assertEquals("GL-5100", result.glCode());
        assertEquals(5, result.usefulLifeYears());
        assertEquals(0.93, result.confidence());
    }
}
