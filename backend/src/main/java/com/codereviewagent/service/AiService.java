package com.codereviewagent.service;

import com.codereviewagent.dto.CodeReviewRequestDto;
import com.codereviewagent.dto.CodeReviewResultDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface AiService {
    CodeReviewResultDto reviewCode(CodeReviewRequestDto request);
    CodeReviewResultDto uploadAndReviewFile(MultipartFile file, UUID projectId, UUID authorId);
}
