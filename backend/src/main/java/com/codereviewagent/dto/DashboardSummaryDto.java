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
public class DashboardSummaryDto {
    private long totalReviews;
    private double improvementScore;
    private long memoryLearned;
    private List<String> languagesReviewed;
    private long activeProjects;
    private List<ReviewDto> recentReviews;
    private long unreadNotificationsCount;
}
