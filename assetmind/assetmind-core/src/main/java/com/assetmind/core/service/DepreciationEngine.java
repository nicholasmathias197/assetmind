package com.assetmind.core.service;

import com.assetmind.core.domain.DepreciationRequest;
import com.assetmind.core.domain.ScheduleLine;

import java.util.List;

public interface DepreciationEngine {
    List<ScheduleLine> calculateSchedule(DepreciationRequest request);
}

