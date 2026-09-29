package com.codereviewagent.entity;

import com.codereviewagent.entity.enums.ReviewStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "Code submission is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private CodeSubmission codeSubmission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Column(name = "is_ai_generated", nullable = false)
    @Builder.Default
    private boolean isAiGenerated = true;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "feedback_comments", columnDefinition = "TEXT")
    private String feedbackComments;

    @NotNull(message = "Verdict is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ReviewStatus verdict = ReviewStatus.COMMENTED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private ReviewMetric reviewMetric;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
