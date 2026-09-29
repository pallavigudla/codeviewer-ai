package com.codereviewagent.dto;

import com.codereviewagent.entity.enums.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewRequestDto {

    @NotNull(message = "Submission ID is required")
    private UUID submissionId;

    private UUID reviewerId;

    private boolean isAiGenerated;

    private String summary;

    private String feedbackComments;

    @NotNull(message = "Verdict is required")
    private ReviewStatus verdict;
}
