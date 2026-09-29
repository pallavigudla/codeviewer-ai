package com.codereviewagent.service;

import com.codereviewagent.dto.ReviewDto;
import com.codereviewagent.dto.ReviewMetricDto;
import com.codereviewagent.entity.Review;
import com.codereviewagent.entity.ReviewMetric;
import com.codereviewagent.entity.User;
import com.codereviewagent.exception.ResourceNotFoundException;
import com.codereviewagent.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewDto> getCurrentUserReviews() {
        User user = authenticatedUserService.getCurrentUser();
        List<Review> reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return reviews.stream().map(this::mapToDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewDto getReviewByIdForCurrentUser(UUID reviewId) {
        User user = authenticatedUserService.getCurrentUser();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId));

        // Enforce user ownership isolation (returns 404 so existence is not leaked to another user)
        if (review.getReviewer() == null || !review.getReviewer().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Review not found with ID: " + reviewId);
        }

        return mapToDto(review);
    }

    public ReviewDto mapToDto(Review review) {
        ReviewMetricDto metricDto = null;
        if (review.getReviewMetric() != null) {
            ReviewMetric rm = review.getReviewMetric();
            metricDto = ReviewMetricDto.builder()
                    .id(rm.getId())
                    .reviewId(review.getId())
                    .codeQualityScore(rm.getCodeQualityScore())
                    .securityScore(rm.getSecurityScore())
                    .performanceScore(rm.getPerformanceScore())
                    .totalIssuesFound(rm.getTotalIssuesFound())
                    .criticalIssues(rm.getCriticalIssues())
                    .warningIssues(rm.getWarningIssues())
                    .infoIssues(rm.getInfoIssues())
                    .createdAt(rm.getCreatedAt())
                    .build();
        }

        return ReviewDto.builder()
                .id(review.getId())
                .submissionId(review.getCodeSubmission() != null ? review.getCodeSubmission().getId() : null)
                .reviewerId(review.getReviewer() != null ? review.getReviewer().getId() : null)
                .isAiGenerated(review.isAiGenerated())
                .summary(review.getSummary())
                .feedbackComments(review.getFeedbackComments())
                .verdict(review.getVerdict())
                .createdAt(review.getCreatedAt())
                .reviewMetric(metricDto)
                .build();
    }
}
