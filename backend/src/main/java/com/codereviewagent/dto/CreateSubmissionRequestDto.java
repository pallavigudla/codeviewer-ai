package com.codereviewagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubmissionRequestDto {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotNull(message = "Author ID is required")
    private UUID authorId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String commitHash;

    private String branchName;

    private String prUrl;

    private String language;

    @NotBlank(message = "Diff content is required")
    private String diffContent;
}
