package com.codereviewagent.controller;

import com.codereviewagent.dto.ApiResponseDto;
import com.codereviewagent.dto.DashboardSummaryDto;
import com.codereviewagent.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping({"/dashboard", "/api/dashboard"})
    public ResponseEntity<ApiResponseDto<DashboardSummaryDto>> getDashboard() {
        DashboardSummaryDto summary = dashboardService.getCurrentUserDashboardSummary();
        return ResponseEntity.ok(ApiResponseDto.success("Dashboard metrics retrieved successfully", summary));
    }
}
