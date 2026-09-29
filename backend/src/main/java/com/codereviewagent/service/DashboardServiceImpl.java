package com.codereviewagent.service;

import com.codereviewagent.dto.DashboardSummaryDto;
import com.codereviewagent.dto.ReviewDto;
import com.codereviewagent.entity.Review;
import com.codereviewagent.entity.User;
import com.codereviewagent.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ReviewRepository reviewRepository;
    private final ReviewMetricRepository reviewMetricRepository;
    private final CodeSubmissionRepository codeSubmissionRepository;
    private final ProjectRepository projectRepository;
    private final NotificationRepository notificationRepository;
    private final ReviewServiceImpl reviewServiceImpl;
    private final AuthenticatedUserService authenticatedUserService;
    private final MemoryService memoryService;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryDto getCurrentUserDashboardSummary() {
        User user = authenticatedUserService.getCurrentUser();
        UUID userId = user.getId();

        long totalReviews = reviewRepository.countByUserId(userId);
        double improvementScore = 85.0;

        Double avgScore = reviewMetricRepository.findAverageQualityScoreByUserId(userId);
        if (avgScore != null) {
            improvementScore = Math.round(avgScore * 10.0) / 10.0;
        }

        long activeProjects = projectRepository.countActiveProjectsByUserId(userId);
        long unreadNotifications = notificationRepository.countByUserIdAndIsReadFalse(userId);
        List<String> languages = codeSubmissionRepository.findDistinctLanguagesByAuthorId(userId);
        List<Review> recentReviewsEntities = reviewRepository.findRecentByUserId(userId, PageRequest.of(0, 5));

        if (languages == null || languages.isEmpty()) {
            languages = List.of("Java", "Python", "JavaScript");
        }

        long memoryLearned = memoryService.retrieveMemory(userId.toString(), null).size();

        List<ReviewDto> recentReviews = recentReviewsEntities.stream()
                .map(reviewServiceImpl::mapToDto)
                .toList();

        return DashboardSummaryDto.builder()
                .totalReviews(totalReviews)
                .improvementScore(improvementScore)
                .memoryLearned(memoryLearned)
                .languagesReviewed(languages)
                .activeProjects(activeProjects)
                .recentReviews(recentReviews)
                .unreadNotificationsCount(unreadNotifications)
                .build();
    }
}
