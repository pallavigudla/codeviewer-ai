package com.codereviewagent.controller;

import com.codereviewagent.dto.ApiResponseDto;
import com.codereviewagent.dto.CodeReviewRequestDto;
import com.codereviewagent.dto.CodeReviewResultDto;
import com.codereviewagent.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AiReviewController {

    private final AiService aiService;

    @PostMapping({"/review", "/api/ai/review", "/api/reviews/analyze"})
    public ResponseEntity<ApiResponseDto<CodeReviewResultDto>> reviewCode(@Valid @RequestBody CodeReviewRequestDto request) {
        CodeReviewResultDto result = aiService.reviewCode(request);
        return new ResponseEntity<>(ApiResponseDto.success("Code review generated successfully", result), HttpStatus.CREATED);
    }

    @PostMapping(value = {"/upload", "/api/ai/upload", "/api/reviews/upload"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseDto<CodeReviewResultDto>> uploadAndReviewFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "projectId", required = false) UUID projectId) {
        CodeReviewResultDto result = aiService.uploadAndReviewFile(file, projectId, null);
        return new ResponseEntity<>(ApiResponseDto.success("Uploaded file analyzed successfully", result), HttpStatus.CREATED);
    }
}
