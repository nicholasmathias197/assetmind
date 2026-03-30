package com.assetmind.ai.service;

import com.assetmind.ai.model.BreakoutSuggestion;
import com.assetmind.ai.model.BreakoutSuggestion.ComponentSuggestion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BreakoutSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(BreakoutSuggestionService.class);

    private static final String SYSTEM_PROMPT = """
            You are an expert fixed-asset accountant specializing in cost segregation and purchase breakouts.
            Given an invoice description of a purchase, break it down into its component assets for depreciation purposes.
            
            Respond ONLY with a valid JSON object — no markdown, no explanation, just raw JSON.
            
            Required format:
            {
              "components": [
                {
                  "description": "<component name>",
                  "assetClass": "<COMPUTER_EQUIPMENT|FURNITURE|LEASEHOLD_IMPROVEMENT|BUILDING_IMPROVEMENT|VEHICLE|OTHER>",
                  "costPercentage": <percentage of total cost, number between 0 and 100>,
                  "usefulLifeYears": <integer>
                }
              ],
              "confidence": <number between 0.0 and 1.0>,
              "rationale": "<brief explanation of the breakout logic>"
            }
            
            Rules:
            - Component costPercentage values MUST sum to exactly 100.
            - Use realistic useful life values: COMPUTER_EQUIPMENT=5, FURNITURE=7, LEASEHOLD_IMPROVEMENT=15, BUILDING_IMPROVEMENT=15-39, VEHICLE=5, OTHER=10.
            - Identify as many distinct depreciable components as appropriate (typically 3-10).
            - Group minor items when individual breakdown is not practical.
            """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BreakoutSuggestionService(@Autowired(required = false) ChatClient chatClient) {
        this.chatClient = chatClient;
        if (chatClient != null) {
            log.info("AI breakout suggestion enabled via ChatClient (Groq)");
        } else {
            log.info("No AI ChatClient configured — breakout suggestions will use keyword fallback. "
                    + "Set GROQ_API_KEY to enable AI.");
        }
    }

    public BreakoutSuggestion suggest(String documentText) {
        if (chatClient != null) {
            try {
                return suggestWithAi(documentText);
            } catch (Exception e) {
                log.warn("AI breakout suggestion failed [{}] — falling back to keyword matching", e.getMessage());
            }
        }
        return suggestWithKeywords(documentText);
    }

    // -------------------------------------------------------------------------
    // AI path
    // -------------------------------------------------------------------------

    private BreakoutSuggestion suggestWithAi(String documentText) throws Exception {
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage("Break down this purchase into component assets:\n\n" + documentText)
        ));

        String raw = chatClient.call(prompt).getResult().getOutput().getContent();
        String json = stripMarkdownFences(raw);
        JsonNode node = objectMapper.readTree(json);

        List<ComponentSuggestion> components = new ArrayList<>();
        for (JsonNode comp : node.get("components")) {
            components.add(new ComponentSuggestion(
                    comp.get("description").asText(),
                    comp.get("assetClass").asText(),
                    comp.get("costPercentage").asDouble(),
                    comp.get("usefulLifeYears").asInt()
            ));
        }

        double confidence = node.path("confidence").asDouble(0.75);
        String rationale = node.path("rationale").asText("AI-generated breakout");

        log.debug("AI suggested {} components (confidence={})", components.size(), confidence);
        return new BreakoutSuggestion(components, confidence, rationale, "AI_GROQ");
    }

    // -------------------------------------------------------------------------
    // Keyword fallback path
    // -------------------------------------------------------------------------

    private BreakoutSuggestion suggestWithKeywords(String documentText) {
        String text = documentText == null ? "" : documentText.toLowerCase();

        if (text.contains("property") || text.contains("building") || text.contains("commercial")
                || text.contains("warehouse") || text.contains("office building")) {
            return new BreakoutSuggestion(List.of(
                    new ComponentSuggestion("Building structure", "BUILDING_IMPROVEMENT", 50, 39),
                    new ComponentSuggestion("Roof", "BUILDING_IMPROVEMENT", 8, 20),
                    new ComponentSuggestion("HVAC system", "BUILDING_IMPROVEMENT", 10, 15),
                    new ComponentSuggestion("Electrical system", "BUILDING_IMPROVEMENT", 8, 15),
                    new ComponentSuggestion("Plumbing", "BUILDING_IMPROVEMENT", 5, 15),
                    new ComponentSuggestion("Parking / paving", "LEASEHOLD_IMPROVEMENT", 4, 15),
                    new ComponentSuggestion("Land", "OTHER", 15, 0)
            ), 0.70, "Keyword-based commercial property breakout template", "RULE_FALLBACK");
        }

        if (text.contains("office") || text.contains("buildout") || text.contains("build-out")
                || text.contains("renovation") || text.contains("tenant improvement")) {
            return new BreakoutSuggestion(List.of(
                    new ComponentSuggestion("Interior walls & framing", "LEASEHOLD_IMPROVEMENT", 30, 15),
                    new ComponentSuggestion("Flooring", "LEASEHOLD_IMPROVEMENT", 15, 10),
                    new ComponentSuggestion("Electrical & lighting", "BUILDING_IMPROVEMENT", 20, 15),
                    new ComponentSuggestion("HVAC modifications", "BUILDING_IMPROVEMENT", 15, 15),
                    new ComponentSuggestion("Furniture & fixtures", "FURNITURE", 12, 7),
                    new ComponentSuggestion("IT / network cabling", "COMPUTER_EQUIPMENT", 8, 5)
            ), 0.68, "Keyword-based office build-out breakout template", "RULE_FALLBACK");
        }

        if (text.contains("data center") || text.contains("server room") || text.contains("it infrastructure")) {
            return new BreakoutSuggestion(List.of(
                    new ComponentSuggestion("Servers & compute hardware", "COMPUTER_EQUIPMENT", 40, 5),
                    new ComponentSuggestion("Networking equipment", "COMPUTER_EQUIPMENT", 15, 5),
                    new ComponentSuggestion("UPS / power systems", "COMPUTER_EQUIPMENT", 10, 7),
                    new ComponentSuggestion("Cooling / HVAC", "BUILDING_IMPROVEMENT", 15, 15),
                    new ComponentSuggestion("Cabling & infrastructure", "LEASEHOLD_IMPROVEMENT", 10, 10),
                    new ComponentSuggestion("Racks & enclosures", "FURNITURE", 10, 7)
            ), 0.65, "Keyword-based data center / IT infrastructure breakout template", "RULE_FALLBACK");
        }

        if (text.contains("vehicle") || text.contains("fleet") || text.contains("truck")
                || text.contains("van") || text.contains("delivery")) {
            return new BreakoutSuggestion(List.of(
                    new ComponentSuggestion("Vehicle", "VEHICLE", 75, 5),
                    new ComponentSuggestion("Vehicle modifications / upfit", "VEHICLE", 15, 5),
                    new ComponentSuggestion("GPS / telematics equipment", "COMPUTER_EQUIPMENT", 5, 5),
                    new ComponentSuggestion("Branding / signage", "OTHER", 5, 3)
            ), 0.65, "Keyword-based vehicle / fleet breakout template", "RULE_FALLBACK");
        }

        // Generic fallback
        return new BreakoutSuggestion(List.of(
                new ComponentSuggestion("Primary asset", "OTHER", 70, 10),
                new ComponentSuggestion("Installation / setup", "OTHER", 15, 10),
                new ComponentSuggestion("Ancillary components", "OTHER", 15, 7)
        ), 0.40, "No strong keyword match — using generic breakout template", "RULE_FALLBACK");
    }

    private String stripMarkdownFences(String raw) {
        return raw.replaceAll("(?s)```[a-zA-Z]*\\s*", "").replace("```", "").trim();
    }
}
