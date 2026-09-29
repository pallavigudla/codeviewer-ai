package com.codereviewagent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeReviewRequestDto {

    private String codeContent;

    private String language;

    private String title;

    private String description;

    private UUID projectId;

    private UUID authorId;

    public void setCodeSnippet(String codeSnippet) {
        if (this.codeContent == null || this.codeContent.isBlank()) {
            this.codeContent = codeSnippet;
        }
    }

    public String getCodeSnippet() {
        return this.codeContent;
    }

    public String getCodeContent() {
        return this.codeContent;
    }
}
