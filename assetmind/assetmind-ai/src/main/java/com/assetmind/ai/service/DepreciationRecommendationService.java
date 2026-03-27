package com.assetmind.ai.service;

import com.assetmind.ai.model.DepreciationRecommendation;
import com.assetmind.core.domain.AssetClass;
import com.assetmind.core.domain.DepreciationMethod;
import com.assetmind.core.service.TaxStrategyAdvisor;
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

import java.util.List;
import java.util.Locale;

@Service
public class DepreciationRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(DepreciationRecommendationService.class);

    private static final String SYSTEM_PROMPT = """
            You are a fixed-asset depreciation advisor.
            Use state and equipment type as context and recommend a method and useful life.
            Respond with ONLY valid JSON:
            {
              "recommendedMethod": "<STRAIGHT_LINE|MACRS_200DB_HY|ADS_STRAIGHT_LINE>",
              "suggestedUsefulLifeYears": <integer>,
              "confidence": <0.0-1.0>,
              "rationale": "<short explanation>"
            }
            """;

    private final ChatClient chatClient;
    private final TaxStrategyAdvisor fallbackAdvisor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DepreciationRecommendationService(
            @Autowired(required = false) ChatClient chatClient,
            TaxStrategyAdvisor fallbackAdvisor
    ) {
        this.chatClient = chatClient;
        this.fallbackAdvisor = fallbackAdvisor;
    }

    public DepreciationRecommendation recommend(
            String stateCode,
            String equipmentType,
            AssetClass assetClass,
            boolean immediateDeductionPreferred,
            boolean longHorizonAsset
    ) {
        if (chatClient != null) {
            try {
                return recommendWithAi(stateCode, equipmentType, assetClass, immediateDeductionPreferred, longHorizonAsset);
            } catch (Exception e) {
                log.warn("AI depreciation recommendation failed [{}], using fallback", e.getMessage());
            }
        }
        return fallbackRecommendation(stateCode, equipmentType, assetClass, immediateDeductionPreferred, longHorizonAsset);
    }

    private DepreciationRecommendation recommendWithAi(
            String stateCode,
            String equipmentType,
            AssetClass assetClass,
            boolean immediateDeductionPreferred,
            boolean longHorizonAsset
    ) throws Exception {
        String userPrompt = String.format(Locale.ROOT,
                "State: %s\nEquipment type: %s\nAsset class: %s\nImmediate deduction preferred: %s\nLong horizon asset: %s",
                normalize(stateCode),
                equipmentType,
                assetClass,
                immediateDeductionPreferred,
                longHorizonAsset);

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage(userPrompt)
        ));

        String raw = chatClient.call(prompt).getResult().getOutput().getContent();
        String json = stripMarkdownFences(raw);
        JsonNode node = objectMapper.readTree(json);

        DepreciationMethod method = DepreciationMethod.valueOf(node.get("recommendedMethod").asText());
        int usefulLife = node.path("suggestedUsefulLifeYears").asInt(defaultUsefulLife(assetClass));
        double confidence = node.path("confidence").asDouble(0.7d);
        String rationale = node.path("rationale").asText("AI recommendation");

        return new DepreciationRecommendation(method, usefulLife, confidence, rationale, "AI_GROQ");
    }

    private DepreciationRecommendation fallbackRecommendation(
            String stateCode,
            String equipmentType,
            AssetClass assetClass,
            boolean immediateDeductionPreferred,
            boolean longHorizonAsset
    ) {
        DepreciationMethod method = fallbackAdvisor.recommendMethod(immediateDeductionPreferred, longHorizonAsset);
        int usefulLife = defaultUsefulLife(assetClass);
        String rationale = String.format(Locale.ROOT,
                "Deterministic fallback recommendation for %s in %s using asset class %s",
                equipmentType,
                normalize(stateCode),
                assetClass);
        return new DepreciationRecommendation(method, usefulLife, 0.60d, rationale, "RULE_FALLBACK");
    }

    private int defaultUsefulLife(AssetClass assetClass) {
        return switch (assetClass) {
            case COMPUTER_EQUIPMENT, VEHICLE -> 5;
            case FURNITURE -> 7;
            case LEASEHOLD_IMPROVEMENT -> 15;
            case BUILDING_IMPROVEMENT -> 39;
            case OTHER -> 10;
        };
    }

    private String stripMarkdownFences(String raw) {
        return raw.replaceAll("(?s)```[a-zA-Z]*\\s*", "").replace("```", "").trim();
    }

    private String normalize(String stateCode) {
        if (stateCode == null) {
            return "UNKNOWN";
        }
        return stateCode.trim().toUpperCase(Locale.ROOT);
    }
}

