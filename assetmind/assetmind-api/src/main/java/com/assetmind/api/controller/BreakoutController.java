package com.assetmind.api.controller;

import com.assetmind.ai.model.BreakoutSuggestion;
import com.assetmind.ai.service.BreakoutSuggestionService;
import com.assetmind.api.dto.BreakoutSuggestRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/breakout")
public class BreakoutController {

    private final BreakoutSuggestionService breakoutSuggestionService;

    public BreakoutController(BreakoutSuggestionService breakoutSuggestionService) {
        this.breakoutSuggestionService = breakoutSuggestionService;
    }

    @PostMapping("/suggest")
    public BreakoutSuggestion suggest(@Valid @RequestBody BreakoutSuggestRequest request) {
        return breakoutSuggestionService.suggest(request.documentText());
    }
}
