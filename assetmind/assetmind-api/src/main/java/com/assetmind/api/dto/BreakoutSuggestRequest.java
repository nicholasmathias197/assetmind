package com.assetmind.api.dto;

import jakarta.validation.constraints.NotBlank;

public record BreakoutSuggestRequest(
        @NotBlank String documentText
) {
}
