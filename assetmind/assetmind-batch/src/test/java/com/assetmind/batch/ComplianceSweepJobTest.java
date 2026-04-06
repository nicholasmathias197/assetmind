package com.assetmind.batch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ComplianceSweepJobTest {

    @Test
    void runNightlyComplianceSweepDoesNotThrow() {
        ComplianceSweepJob job = new ComplianceSweepJob();
        assertDoesNotThrow(job::runNightlyComplianceSweep);
    }
}
