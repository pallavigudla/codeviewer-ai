package com.codereviewagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImprovementAnalysisDto {
    private List<String> improvedAreas;
    private List<String> repeatedMistakes;
    private List<String> newMistakes;
    private Double memoryConfidence;
    private Double improvementPercentage;
    private Double overallQualityScore;
}
