package com.assetmind.core.service;

import com.assetmind.core.domain.DepreciationMethod;
import org.springframework.stereotype.Service;

@Service
public class TaxStrategyAdvisor {

    public DepreciationMethod recommendMethod(boolean immediateDeductionPreferred, boolean longHorizonAsset) {
        if (immediateDeductionPreferred) {
            return DepreciationMethod.MACRS_200DB_HY;
        }
        return longHorizonAsset ? DepreciationMethod.ADS_STRAIGHT_LINE : DepreciationMethod.STRAIGHT_LINE;
    }
}
