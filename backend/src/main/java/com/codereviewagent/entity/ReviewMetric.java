package com.codereviewagent.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "review_metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "Review is required")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false, unique = true)
    private Review review;

    @Column(name = "code_quality_score")
    private Double codeQualityScore;

    @Column(name = "security_score")
    private Double securityScore;

    @Column(name = "performance_score")
    private Double performanceScore;

    @Column(name = "total_issues_found")
    @Builder.Default
    private Integer totalIssuesFound = 0;

    @Column(name = "critical_issues")
    @Builder.Default
    private Integer criticalIssues = 0;

    @Column(name = "warning_issues")
    @Builder.Default
    private Integer warningIssues = 0;

    @Column(name = "info_issues")
    @Builder.Default
    private Integer infoIssues = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
