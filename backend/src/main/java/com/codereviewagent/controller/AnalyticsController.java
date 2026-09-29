package com.codereviewagent.controller;

import com.codereviewagent.dto.AnalyticsResponseDto;
import com.codereviewagent.dto.ApiResponseDto;
import com.codereviewagent.dto.MemoryGrowthDto;
import com.codereviewagent.service.ImprovementEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AnalyticsController {

    private final ImprovementEngineService improvementEngineService;

    @GetMapping({"/analytics", "/api/analytics"})
    public ResponseEntity<ApiResponseDto<AnalyticsResponseDto>> getUserAnalytics() {
        AnalyticsResponseDto analytics = improvementEngineService.getUserAnalytics();
        return ResponseEntity.ok(ApiResponseDto.success("Improvement analytics retrieved successfully", analytics));
    }

    @GetMapping({"/memory-growth", "/api/memory-growth"})
    public ResponseEntity<ApiResponseDto<MemoryGrowthDto>> getMemoryGrowthAnalytics() {
        MemoryGrowthDto memoryGrowth = improvementEngineService.getMemoryGrowthAnalytics();
        return ResponseEntity.ok(ApiResponseDto.success("Memory growth analytics retrieved successfully", memoryGrowth));
    }
}
