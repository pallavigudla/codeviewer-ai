package com.codereviewagent.dto;

import com.codereviewagent.entity.enums.SubmissionStatus;
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
public class CodeSubmissionDto {
    private UUID id;
    private UUID projectId;
    private UUID authorId;
    private String title;
    private String description;
    private String commitHash;
    private String branchName;
    private String prUrl;
    private String language;
    private String diffContent;
    private SubmissionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
