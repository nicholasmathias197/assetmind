package com.assetmind.ai.service;

import com.assetmind.ai.model.ClassificationSuggestion;
import com.assetmind.core.domain.AssetClass;
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

class AssetClassificationServiceTest {

    // -------------------------------------------------------------------------
    // No-AI keyword fallback tests (chatClient == null)
    // -------------------------------------------------------------------------

    @Test
    void keywordFallback_detectsLaptopAsComputerEquipment() {
        AssetClassificationService service = new AssetClassificationService(null);

        ClassificationSuggestion result = service.suggestFromInvoiceText(
                "Invoice for Dell XPS 15 laptop purchase for accounting team");

        assertEquals(AssetClass.COMPUTER_EQUIPMENT, result.assetClass());
        assertEquals("1610", result.glCode());
        assertEquals(5, result.usefulLifeYears());
        assertTrue(result.confidence() > 0.5);
    }

    @Test
    void keywordFallback_detectsServerAsComputerEquipment() {
        AssetClassificationService service = new AssetClassificationService(null);

        ClassificationSuggestion result = service.suggestFromInvoiceText(
                "Dell PowerEdge R750 server rack unit");

        assertEquals(AssetClass.COMPUTER_EQUIPMENT, result.assetClass());
        assertEquals("1610", result.glCode());
    }

    @Test
    void keywordFallback_detectsDeskAsFurniture() {
        AssetClassificationService service = new AssetClassificationService(null);

        ClassificationSuggestion result = service.suggestFromInvoiceText(
                "Standing desk and ergonomic chair for home office");

        assertEquals(AssetClass.FURNITURE, result.assetClass());
        assertEquals("1620", result.glCode());
        assertEquals(7, result.usefulLifeYears());
    }

    @Test
    void keywordFallback_detectsVehicle() {
        AssetClassificationService service = new AssetClassificationService(null);

        ClassificationSuggestion result = service.suggestFromInvoiceText(
                "Ford Transit cargo van for field service team");

        assertEquals(AssetClass.VEHICLE, result.assetClass());
        assertEquals("1630", result.glCode());
        assertEquals(5, result.usefulLifeYears());
    }

    @Test
    void keywordFallback_detectsLeaseholdImprovement() {
        AssetClassificationService service = new AssetClassificationService(null);

        ClassificationSuggestion result = service.suggestFromInvoiceText(
                "Leasehold improvement — interior renovation of office floor 4");

        assertEquals(AssetClass.LEASEHOLD_IMPROVEMENT, result.assetClass());
        assertEquals("1710", result.glCode());
        assertEquals(15, result.usefulLifeYears());
    }

    @Test
    void keywordFallback_detectsBuildingImprovement() {
        AssetClassificationService service = new AssetClassificationService(null);

        ClassificationSuggestion result = service.suggestFromInvoiceText(
                "Roof replacement and HVAC system upgrade");

        assertEquals(AssetClass.BUILDING_IMPROVEMENT, result.assetClass());
        assertEquals("1720", result.glCode());
        assertEquals(39, result.usefulLifeYears());
    }

    @Test
    void keywordFallback_returnsOtherWhenNoKeywordMatches() {
        AssetClassificationService service = new AssetClassificationService(null);

        ClassificationSuggestion result = service.suggestFromInvoiceText(
                "Miscellaneous office supplies purchase");

        assertEquals(AssetClass.OTHER, result.assetClass());
        assertEquals("1699", result.glCode());
        assertEquals(10, result.usefulLifeYears());
    }

    @Test
    void keywordFallback_handlesNullInput() {
        AssetClassificationService service = new AssetClassificationService(null);

        ClassificationSuggestion result = service.suggestFromInvoiceText(null);

        assertEquals(AssetClass.OTHER, result.assetClass());
    }

    @Test
    void keywordFallback_detectsLand() {
        AssetClassificationService service = new AssetClassificationService(null);

        ClassificationSuggestion result = service.suggestFromInvoiceText("Vacant land parcel purchase 2.5 acres");

        assertEquals(AssetClass.LAND, result.assetClass());
        assertEquals("1500", result.glCode());
        assertEquals(0, result.usefulLifeYears());
    }

    @Test
    void keywordFallback_detectsMachinery() {
        AssetClassificationService service = new AssetClassificationService(null);

        ClassificationSuggestion result = service.suggestFromInvoiceText("CNC milling machinery for production line");

        assertEquals(AssetClass.MACHINERY, result.assetClass());
        assertEquals("1640", result.glCode());
        assertEquals(7, result.usefulLifeYears());
    }

    @Test
    void keywordFallback_detectsMonitorAsComputerEquipment() {
        AssetClassificationService service = new AssetClassificationService(null);

        ClassificationSuggestion result = service.suggestFromInvoiceText("Dell 32-inch monitor for design team");

        assertEquals(AssetClass.COMPUTER_EQUIPMENT, result.assetClass());
    }

    @Test
    void keywordFallback_detectsPrinterAsComputerEquipment() {
        AssetClassificationService service = new AssetClassificationService(null);

        ClassificationSuggestion result = service.suggestFromInvoiceText("HP LaserJet Pro printer");

        assertEquals(AssetClass.COMPUTER_EQUIPMENT, result.assetClass());
    }

    @Test
    void keywordFallback_detectsCabinetAsFurniture() {
        AssetClassificationService service = new AssetClassificationService(null);

        ClassificationSuggestion result = service.suggestFromInvoiceText("Filing cabinet for records room");

        assertEquals(AssetClass.FURNITURE, result.assetClass());
    }

    @Test
    void keywordFallback_detectsConveyorAsMachinery() {
        AssetClassificationService service = new AssetClassificationService(null);

        ClassificationSuggestion result = service.suggestFromInvoiceText("Conveyor belt system for warehouse");

        assertEquals(AssetClass.MACHINERY, result.assetClass());
    }

    // -------------------------------------------------------------------------
    // Mocked AI path tests (chatClient != null)
    // -------------------------------------------------------------------------

    @Test
    void aiPath_parsesJsonResponseAndMapsFieldsCorrectly() throws Exception {
        String aiJson = """
                {
                  "assetClass": "COMPUTER_EQUIPMENT",
                  "glCode": "1610",
                  "usefulLifeYears": 5,
                  "confidence": 0.97,
                  "rationale": "Detected laptop purchase from invoice description"
                }
                """;

        ChatClient chatClient = mockChatClientReturning(aiJson);
        AssetClassificationService service = new AssetClassificationService(chatClient);

        ClassificationSuggestion result = service.suggestFromInvoiceText(
                "Invoice for MacBook Pro 14-inch for engineering team");

        assertEquals(AssetClass.COMPUTER_EQUIPMENT, result.assetClass());
        assertEquals("1610", result.glCode());
        assertEquals(5, result.usefulLifeYears());
        assertEquals(0.97, result.confidence(), 0.001);
        assertEquals("Detected laptop purchase from invoice description", result.rationale());
    }

    @Test
    void aiPath_parsesVehicleJsonCorrectly() throws Exception {
        String aiJson = """
                {
                  "assetClass": "VEHICLE",
                  "glCode": "1630",
                  "usefulLifeYears": 5,
                  "confidence": 0.92,
                  "rationale": "Fleet vehicle purchase"
                }
                """;

        ChatClient chatClient = mockChatClientReturning(aiJson);
        AssetClassificationService service = new AssetClassificationService(chatClient);

        ClassificationSuggestion result = service.suggestFromInvoiceText(
                "Toyota Hilux pickup truck");

        assertEquals(AssetClass.VEHICLE, result.assetClass());
        assertEquals("1630", result.glCode());
    }

    @Test
    void aiPath_stripsMarkdownFencesBeforeParsing() throws Exception {
        String aiJsonWithFences = """
                ```json
                {
                  "assetClass": "FURNITURE",
                  "glCode": "1620",
                  "usefulLifeYears": 7,
                  "confidence": 0.88,
                  "rationale": "Office furniture"
                }
                ```
                """;

        ChatClient chatClient = mockChatClientReturning(aiJsonWithFences);
        AssetClassificationService service = new AssetClassificationService(chatClient);

        ClassificationSuggestion result = service.suggestFromInvoiceText("Office chairs and desks");

        assertEquals(AssetClass.FURNITURE, result.assetClass());
        assertEquals("1620", result.glCode());
    }

    @Test
    void aiPath_fallsBackToKeywords_whenChatClientThrowsException() throws Exception {
        ChatClient chatClient = mock(ChatClient.class);
        when(chatClient.call(any(Prompt.class))).thenThrow(new RuntimeException("Groq API timeout"));

        AssetClassificationService service = new AssetClassificationService(chatClient);

        // "laptop" keyword should be picked up by the fallback
        ClassificationSuggestion result = service.suggestFromInvoiceText(
                "laptop computer purchase");

        assertEquals(AssetClass.COMPUTER_EQUIPMENT, result.assetClass());
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

