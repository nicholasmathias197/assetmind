package com.assetmind.api.dto;

import com.assetmind.core.domain.DepreciationMethod;
import com.assetmind.core.domain.ScheduleLine;

import java.util.List;

/**
 * Response from POST /api/v1/depreciation/ai-run.
 * <p>
 * Contains the AI recommendation metadata followed by the full
 * year-by-year depreciation schedule computed from that recommendation.
 */
public record AiDepreciationResponse(

        // ── AI recommendation metadata ────────────────────────────────────────

        /** Depreciation method the AI (or rule engine) selected. */
        DepreciationMethod recommendedMethod,

        /** Useful life in years as determined by the AI (or class default). */
        int suggestedUsefulLifeYears,

        /** 0.0–1.0 confidence score returned by the model. */
        double aiConfidence,

        /** Plain-English explanation of why this method was chosen. */
        String aiRationale,

        /**
         * "AI_GROQ"        – answer came from the Groq LLM.
         * "RULE_FALLBACK"  – GROQ_API_KEY not set; rule engine was used.
         */
        String aiSource,

        // ── Computed depreciation schedule ───────────────────────────────────

        /** Year-by-year depreciation lines; one entry per useful-life year. */
        List<ScheduleLine> schedule
) {
}

