package com.codereviewagent.controller;

import com.codereviewagent.dto.ApiResponseDto;
import com.codereviewagent.dto.ReviewDto;
import com.codereviewagent.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping({"/reviews", "/api/reviews"})
    public ResponseEntity<ApiResponseDto<List<ReviewDto>>> getReviews() {
        List<ReviewDto> reviews = reviewService.getCurrentUserReviews();
        return ResponseEntity.ok(ApiResponseDto.success("Reviews retrieved successfully", reviews));
    }

    @GetMapping({"/review/{id}", "/api/reviews/{id}"})
    public ResponseEntity<ApiResponseDto<ReviewDto>> getReviewById(@PathVariable UUID id) {
        ReviewDto review = reviewService.getReviewByIdForCurrentUser(id);
        return ResponseEntity.ok(ApiResponseDto.success("Review details retrieved successfully", review));
    }
}
