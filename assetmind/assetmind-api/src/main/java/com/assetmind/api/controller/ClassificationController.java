package com.assetmind.api.controller;

import com.assetmind.ai.model.ClassificationSuggestion;
import com.assetmind.ai.service.AssetClassificationService;
import com.assetmind.api.dto.ClassificationRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/classification")
public class ClassificationController {

    private final AssetClassificationService classificationService;

    public ClassificationController(AssetClassificationService classificationService) {
        this.classificationService = classificationService;
    }

    @PostMapping("/suggest")
    public ClassificationSuggestion suggest(@Valid @RequestBody ClassificationRequest request) {
        return classificationService.suggestFromInvoiceText(request.documentText());
    }
}

