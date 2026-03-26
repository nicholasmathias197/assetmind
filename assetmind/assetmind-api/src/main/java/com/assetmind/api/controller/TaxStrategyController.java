package com.assetmind.api.controller;

import com.assetmind.api.dto.TaxStrategyRequest;
import com.assetmind.api.dto.TaxStrategyResponse;
import com.assetmind.core.service.TaxStrategyAdvisor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tax-strategy")
public class TaxStrategyController {

    private final TaxStrategyAdvisor taxStrategyAdvisor;

    public TaxStrategyController(TaxStrategyAdvisor taxStrategyAdvisor) {
        this.taxStrategyAdvisor = taxStrategyAdvisor;
    }

    @PostMapping("/recommend")
    public TaxStrategyResponse recommend(@RequestBody TaxStrategyRequest request) {
        return new TaxStrategyResponse(
                taxStrategyAdvisor.recommendMethod(request.immediateDeductionPreferred(), request.longHorizonAsset())
        );
    }
}

