package com.codereviewagent.service;

import com.codereviewagent.dto.ReviewDto;

import java.util.List;
import java.util.UUID;

public interface ReviewService {
    List<ReviewDto> getCurrentUserReviews();
    ReviewDto getReviewByIdForCurrentUser(UUID reviewId);
}
