package com.assetmind.ai.service;

import com.assetmind.ai.model.ClassificationSuggestion;
import com.assetmind.core.domain.AssetClass;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssetClassificationService {

    private static final Logger log = LoggerFactory.getLogger(AssetClassificationService.class);

    private static final String SYSTEM_PROMPT = """
            You are an expert fixed asset accountant. Given invoice or document text, classify the asset into exactly one category and respond ONLY with a valid JSON object — no markdown, no explanation, just raw JSON.

            Required format:
            {
              "assetClass": "<COMPUTER_EQUIPMENT|FURNITURE|LEASEHOLD_IMPROVEMENT|BUILDING_IMPROVEMENT|VEHICLE|LAND|BUILDING|MACHINERY|OTHER>",
              "glCode": "<GL code>",
              "usefulLifeYears": <integer>,
              "confidence": <number between 0.0 and 1.0>,
              "rationale": "<one sentence explanation>"
            }

            GL codes and standard useful life:
            - COMPUTER_EQUIPMENT  → glCode=1610, usefulLifeYears=5
            - FURNITURE           → glCode=1620, usefulLifeYears=7
            - LEASEHOLD_IMPROVEMENT → glCode=1710, usefulLifeYears=15
            - BUILDING_IMPROVEMENT  → glCode=1720, usefulLifeYears=39
            - VEHICLE             → glCode=1630, usefulLifeYears=5
            - LAND                → glCode=1500, usefulLifeYears=0 (not depreciated)
            - BUILDING            → glCode=1510, usefulLifeYears=39
            - MACHINERY           → glCode=1640, usefulLifeYears=7
            - OTHER               → glCode=1699, usefulLifeYears=10
            """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AssetClassificationService(@Autowired(required = false) ChatClient chatClient) {
        this.chatClient = chatClient;
        if (chatClient != null) {
            log.info("AI asset classification enabled via ChatClient (Groq)");
        } else {
            log.info("No AI ChatClient configured — asset classification will use keyword fallback. "
                    + "Set GROQ_API_KEY to enable AI.");
        }
    }

    @Cacheable(value = "classifications", key = "#documentText")
    public ClassificationSuggestion suggestFromInvoiceText(String documentText) {
        if (chatClient != null) {
            try {
                return classifyWithAi(documentText);
            } catch (Exception e) {
                log.warn("AI classification failed [{}] — falling back to keyword matching", e.getMessage());
            }
        }
        return classifyWithKeywords(documentText);
    }

    // -------------------------------------------------------------------------
    // AI path
    // -------------------------------------------------------------------------

    private ClassificationSuggestion classifyWithAi(String documentText) throws Exception {
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage("Classify this asset document:\n\n" + documentText)
        ));

        String raw = chatClient.call(prompt).getResult().getOutput().getContent();

        // Strip markdown code fences that some models include even when told not to
        String json = raw.replaceAll("(?s)```[a-z]*\\s*", "").replace("```", "").trim();

        JsonNode node = objectMapper.readTree(json);
        AssetClass assetClass = AssetClass.valueOf(node.get("assetClass").asText());
        String glCode             = node.get("glCode").asText();
        int usefulLifeYears       = node.get("usefulLifeYears").asInt();
        double confidence         = node.get("confidence").asDouble();
        String rationale          = node.get("rationale").asText();

        log.debug("AI classified document as {} (confidence={})", assetClass, confidence);
        return new ClassificationSuggestion(assetClass, glCode, usefulLifeYears, confidence, rationale);
    }

    // -------------------------------------------------------------------------
    // Keyword fallback path (always available, no external dependency)
    // -------------------------------------------------------------------------

    private ClassificationSuggestion classifyWithKeywords(String documentText) {
        String text = documentText == null ? "" : documentText.toLowerCase();

        if (text.contains("laptop") || text.contains("server") || text.contains("workstation")
                || text.contains("computer") || text.contains("monitor") || text.contains("printer")) {
            return new ClassificationSuggestion(AssetClass.COMPUTER_EQUIPMENT, "1610", 5, 0.91,
                    "Detected IT equipment keywords from invoice text");
        }
        if (text.contains("desk") || text.contains("chair") || text.contains("furniture")
                || text.contains("cabinet") || text.contains("shelf")) {
            return new ClassificationSuggestion(AssetClass.FURNITURE, "1620", 7, 0.87,
                    "Detected office furniture keywords from invoice text");
        }
        if (text.contains("leasehold") || text.contains("tenant improvement")
                || text.contains("interior renovation")) {
            return new ClassificationSuggestion(AssetClass.LEASEHOLD_IMPROVEMENT, "1710", 15, 0.82,
                    "Detected leasehold improvement terms from invoice text");
        }
        if (text.contains("building") || text.contains("structural") || text.contains("roof")
                || text.contains("hvac")) {
            return new ClassificationSuggestion(AssetClass.BUILDING_IMPROVEMENT, "1720", 39, 0.80,
                    "Detected building improvement terms from invoice text");
        }
        if (text.contains("land") || text.contains("acreage") || text.contains("lot")
                || text.contains("parcel")) {
            return new ClassificationSuggestion(AssetClass.LAND, "1500", 0, 0.85,
                    "Detected land / real property terms from invoice text");
        }
        if (text.contains("machinery") || text.contains("cnc") || text.contains("press")
                || text.contains("lathe") || text.contains("conveyor")) {
            return new ClassificationSuggestion(AssetClass.MACHINERY, "1640", 7, 0.83,
                    "Detected machinery / manufacturing equipment keywords from invoice text");
        }
        if (text.contains("vehicle") || text.contains("truck") || text.contains("van")
                || text.contains("car") || text.contains("automobile")) {
            return new ClassificationSuggestion(AssetClass.VEHICLE, "1630", 5, 0.85,
                    "Detected vehicle keywords from invoice text");
        }

        return new ClassificationSuggestion(AssetClass.OTHER, "1699", 10, 0.52,
                "Fallback classification — no strong keyword match found");
    }
}
