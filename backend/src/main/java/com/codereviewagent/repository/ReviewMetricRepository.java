package com.codereviewagent.repository;

import com.codereviewagent.entity.ReviewMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewMetricRepository extends JpaRepository<ReviewMetric, UUID> {
    Optional<ReviewMetric> findByReviewId(UUID reviewId);

    @Query("SELECT AVG(rm.codeQualityScore) FROM ReviewMetric rm WHERE rm.review.reviewer.id = :userId OR rm.review.codeSubmission.author.id = :userId")
    Double findAverageQualityScoreByUserId(@Param("userId") UUID userId);

    @Query("SELECT AVG(rm.codeQualityScore) FROM ReviewMetric rm")
    Double findGlobalAverageQualityScore();
}
