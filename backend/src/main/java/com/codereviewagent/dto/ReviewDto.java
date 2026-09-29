package com.codereviewagent.dto;

import com.codereviewagent.entity.enums.ReviewStatus;
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
public class ReviewDto {
    private UUID id;
    private UUID submissionId;
    private UUID reviewerId;
    private boolean isAiGenerated;
    private String summary;
    private String feedbackComments;
    private ReviewStatus verdict;
    private LocalDateTime createdAt;
    private ReviewMetricDto reviewMetric;

    public ReviewMetricDto getMetrics() {
        return reviewMetric;
    }
}
