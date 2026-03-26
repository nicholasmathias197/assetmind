package com.assetmind.api.controller;

import com.assetmind.core.domain.DepreciationRequest;
import com.assetmind.core.domain.ScheduleLine;
import com.assetmind.core.service.DepreciationEngine;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/depreciation")
public class DepreciationController {

    private final DepreciationEngine depreciationEngine;

    public DepreciationController(DepreciationEngine depreciationEngine) {
        this.depreciationEngine = depreciationEngine;
    }

    @PostMapping("/run")
    public List<ScheduleLine> run(@Valid @RequestBody DepreciationRequest request) {
        return depreciationEngine.calculateSchedule(request);
    }
}

