package com.assetmind.ai.service;

import com.assetmind.ai.model.TaxStrategyRecommendation;
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
public class TaxStrategyRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(TaxStrategyRecommendationService.class);

    private static final String SYSTEM_PROMPT = """
            You are a fixed-asset and tax strategy assistant.
            Recommend a depreciation method based on state and equipment type context.
            Respond with ONLY valid JSON:
            {
              "recommendedMethod": "<STRAIGHT_LINE|MACRS_200DB_HY|ADS_STRAIGHT_LINE>",
              "confidence": <0.0-1.0>,
              "rationale": "<short explanation>"
            }
            """;

    private final ChatClient chatClient;
    private final TaxStrategyAdvisor fallbackAdvisor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TaxStrategyRecommendationService(
            @Autowired(required = false) ChatClient chatClient,
            TaxStrategyAdvisor fallbackAdvisor
    ) {
        this.chatClient = chatClient;
        this.fallbackAdvisor = fallbackAdvisor;
    }

    public TaxStrategyRecommendation recommend(
            String stateCode,
            String equipmentType,
            boolean immediateDeductionPreferred,
            boolean longHorizonAsset
    ) {
        if (chatClient != null) {
            try {
                return recommendWithAi(stateCode, equipmentType, immediateDeductionPreferred, longHorizonAsset);
            } catch (Exception e) {
                log.warn("AI tax recommendation failed [{}], using fallback", e.getMessage());
            }
        }
        return fallbackRecommendation(immediateDeductionPreferred, longHorizonAsset, stateCode, equipmentType);
    }

    private TaxStrategyRecommendation recommendWithAi(
            String stateCode,
            String equipmentType,
            boolean immediateDeductionPreferred,
            boolean longHorizonAsset
    ) throws Exception {
        String userPrompt = String.format(Locale.ROOT,
                "State: %s\nEquipment type: %s\nImmediate deduction preferred: %s\nLong horizon asset: %s",
                normalize(stateCode),
                equipmentType,
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
        double confidence = node.path("confidence").asDouble(0.7d);
        String rationale = node.path("rationale").asText("AI recommendation");

        return new TaxStrategyRecommendation(method, confidence, rationale, "AI_GROQ");
    }

    private TaxStrategyRecommendation fallbackRecommendation(
            boolean immediateDeductionPreferred,
            boolean longHorizonAsset,
            String stateCode,
            String equipmentType
    ) {
        DepreciationMethod method = fallbackAdvisor.recommendMethod(immediateDeductionPreferred, longHorizonAsset);
        String rationale = String.format(Locale.ROOT,
                "Deterministic fallback recommendation for %s equipment in %s based on deduction and horizon flags",
                equipmentType,
                normalize(stateCode));
        return new TaxStrategyRecommendation(method, 0.60d, rationale, "RULE_FALLBACK");
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

