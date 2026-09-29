package com.codereviewagent.controller;

import com.codereviewagent.dto.ApiResponseDto;
import com.codereviewagent.dto.MemoryItemDto;
import com.codereviewagent.service.MemoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryService memoryService;

    @GetMapping({"/memory", "/api/memory"})
    public ResponseEntity<ApiResponseDto<List<MemoryItemDto>>> getAllMemories(
            @RequestParam(required = false) String language) {
        List<MemoryItemDto> memories = memoryService.retrieveMemory(null, language);
        return ResponseEntity.ok(ApiResponseDto.success("Hindsight memories retrieved successfully", memories));
    }

    @PostMapping({"/memory", "/api/memory", "/memory/save", "/api/memory/save"})
    public ResponseEntity<ApiResponseDto<MemoryItemDto>> saveMemory(@Valid @RequestBody MemoryItemDto request) {
        MemoryItemDto saved = memoryService.saveMemory(request);
        return new ResponseEntity<>(ApiResponseDto.success("Hindsight memory saved successfully", saved), HttpStatus.CREATED);
    }

    @GetMapping({"/memory/history", "/api/memory/history"})
    public ResponseEntity<ApiResponseDto<List<MemoryItemDto>>> getMemoryHistory(
            @RequestParam(required = false) String language) {
        List<MemoryItemDto> memories = memoryService.retrieveMemory(null, language);
        return ResponseEntity.ok(ApiResponseDto.success("Hindsight memories retrieved successfully", memories));
    }

    @GetMapping({"/memory/search", "/api/memory/search"})
    public ResponseEntity<ApiResponseDto<List<MemoryItemDto>>> searchMemory(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String language) {
        List<MemoryItemDto> results = memoryService.searchMemory(query, null, language);
        return ResponseEntity.ok(ApiResponseDto.success("Hindsight memory search completed", results));
    }

    @PutMapping({"/memory/{id}", "/api/memory/{id}"})
    public ResponseEntity<ApiResponseDto<MemoryItemDto>> updateMemory(
            @PathVariable String id,
            @RequestBody MemoryItemDto request) {
        MemoryItemDto updated = memoryService.updateMemory(id, request);
        return ResponseEntity.ok(ApiResponseDto.success("Hindsight memory entry updated successfully", updated));
    }
}
