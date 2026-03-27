package com.assetmind.api.dto;

import com.assetmind.core.domain.AssetClass;
import com.assetmind.core.domain.BookType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Single-shot request that lets the AI choose the depreciation method and
 * useful-life years, then immediately runs the full depreciation schedule.
 * <p>
 * Endpoint: POST /api/v1/depreciation/ai-run
 */
public record AiDepreciationRequest(

        /** Caller-supplied asset identifier (used to label schedule lines). */
        @NotBlank String assetId,

        /** Two-letter US state code used by the AI for state-specific guidance. */
        @NotBlank String stateCode,

        /** Free-text description of the equipment, e.g. "Dell XPS 15 laptop". */
        @NotBlank String equipmentType,

        /** Broad asset category; also drives the useful-life default. */
        @NotNull AssetClass assetClass,

        /** BOOK or TAX (determines which depreciation rules apply). */
        @NotNull BookType bookType,

        /** Date the asset was placed in service. */
        @NotNull LocalDate inServiceDate,

        /** Original purchase cost (must be ≥ 0). */
        @NotNull @DecimalMin("0.0") BigDecimal costBasis,

        /** Expected residual / salvage value at end of useful life (defaults to 0 if omitted). */
        @DecimalMin("0.0") BigDecimal salvageValue,

        /** Whether a Section 179 immediate expensing election is preferred. */
        boolean section179Enabled,

        /** Dollar amount to expense under Section 179 (only used when section179Enabled=true). */
        @DecimalMin("0.0") BigDecimal section179Amount,

        /** First-year bonus depreciation rate, e.g. 0.60 for 60 % bonus (0 = none). */
        @DecimalMin("0.0") BigDecimal bonusDepreciationRate,

        /**
         * Hint to the AI: true = favour accelerated methods (MACRS, Section 179).
         * Ignored when GROQ_API_KEY is not set.
         */
        boolean immediateDeductionPreferred,

        /**
         * Hint to the AI: true = expect a long economic life (favour ADS straight-line).
         * Ignored when GROQ_API_KEY is not set.
         */
        boolean longHorizonAsset
) {
    /** Canonical defaults so callers can omit optional money fields. */
    public BigDecimal salvageValueOrZero() {
        return salvageValue != null ? salvageValue : BigDecimal.ZERO;
    }

    public BigDecimal section179AmountOrZero() {
        return section179Amount != null ? section179Amount : BigDecimal.ZERO;
    }

    public BigDecimal bonusDepreciationRateOrZero() {
        return bonusDepreciationRate != null ? bonusDepreciationRate : BigDecimal.ZERO;
    }
}

