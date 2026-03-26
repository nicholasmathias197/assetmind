package com.assetmind.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ClassificationRequest(
        @NotBlank String documentText
) {
}

