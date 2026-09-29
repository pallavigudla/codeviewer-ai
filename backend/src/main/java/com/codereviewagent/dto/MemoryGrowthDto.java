package com.codereviewagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryGrowthDto {
    private List<String> timelineLabels;
    private List<Long> memoryCounts;
    private Map<String, Long> categoryBreakdown;
    private Map<String, Object> chartData;
}
