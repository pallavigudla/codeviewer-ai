package com.codereviewagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewMetricDto {
    private UUID id;
    private UUID reviewId;
    private Double codeQualityScore;
    private Double securityScore;
    private Double performanceScore;
    private Integer totalIssuesFound;
    private Integer criticalIssues;
    private Integer warningIssues;
    private Integer infoIssues;
    private LocalDateTime createdAt;
}
