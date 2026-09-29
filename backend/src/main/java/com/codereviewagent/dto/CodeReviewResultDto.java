package com.codereviewagent.dto;

import com.codereviewagent.entity.enums.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeReviewResultDto {
    private UUID reviewId;
    private UUID submissionId;
    private String detectedLanguage;
    private List<CodeIssueDto> bugs;
    private List<CodeIssueDto> security;
    private List<CodeIssueDto> performance;
    private List<CodeIssueDto> readability;
    private List<CodeIssueDto> naming;
    private List<CodeIssueDto> bestPractices;
    private List<CodeIssueDto> complexity;
    private List<CodeIssueDto> improvements;
    private Double overallScore;
    private String summary;
    private String feedbackComments;
    private ReviewStatus verdict;
    private ReviewMetricDto metrics;
    private LocalDateTime createdAt;

    public ReviewMetricDto getReviewMetric() {
        return metrics;
    }
}
