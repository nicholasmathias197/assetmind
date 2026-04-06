package com.assetmind.core.service;

import com.assetmind.core.domain.DepreciationMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaxStrategyAdvisorTest {

    private final TaxStrategyAdvisor advisor = new TaxStrategyAdvisor();

    @Test
    void immediateDeductionPreferred_returnsMacrs() {
        assertEquals(DepreciationMethod.MACRS_200DB_HY,
                advisor.recommendMethod(true, false));
    }

    @Test
    void immediateDeductionPreferred_overridesLongHorizon() {
        assertEquals(DepreciationMethod.MACRS_200DB_HY,
                advisor.recommendMethod(true, true));
    }

    @Test
    void longHorizonAsset_returnsAds() {
        assertEquals(DepreciationMethod.ADS_STRAIGHT_LINE,
                advisor.recommendMethod(false, true));
    }

    @Test
    void noPreferences_returnsStraightLine() {
        assertEquals(DepreciationMethod.STRAIGHT_LINE,
                advisor.recommendMethod(false, false));
    }
}
