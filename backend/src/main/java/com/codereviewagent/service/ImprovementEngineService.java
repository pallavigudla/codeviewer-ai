package com.codereviewagent.service;

import com.codereviewagent.dto.AnalyticsResponseDto;
import com.codereviewagent.dto.MemoryGrowthDto;

public interface ImprovementEngineService {
    AnalyticsResponseDto getUserAnalytics();
    MemoryGrowthDto getMemoryGrowthAnalytics();
}
