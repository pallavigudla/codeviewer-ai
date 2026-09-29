package com.codereviewagent.dto;

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
public class ProjectDto {
    private UUID id;
    private String name;
    private String repositoryUrl;
    private String defaultBranch;
    private String description;
    private UUID ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
