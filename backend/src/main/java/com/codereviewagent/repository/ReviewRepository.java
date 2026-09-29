package com.codereviewagent.repository;

import com.codereviewagent.entity.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByCodeSubmissionId(UUID submissionId);
    List<Review> findByReviewerId(UUID reviewerId);

    @Query("SELECT r FROM Review r WHERE r.reviewer.id = :userId OR r.codeSubmission.author.id = :userId ORDER BY r.createdAt DESC")
    List<Review> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    @Query("SELECT r FROM Review r WHERE r.reviewer.id = :userId OR r.codeSubmission.author.id = :userId ORDER BY r.createdAt DESC")
    List<Review> findRecentByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.reviewer.id = :userId OR r.codeSubmission.author.id = :userId")
    long countByUserId(@Param("userId") UUID userId);
}
