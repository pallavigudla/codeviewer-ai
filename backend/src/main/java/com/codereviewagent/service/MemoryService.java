package com.codereviewagent.service;

import com.codereviewagent.dto.MemoryItemDto;

import java.util.List;

public interface MemoryService {
    MemoryItemDto saveMemory(MemoryItemDto memoryDto);
    List<MemoryItemDto> retrieveMemory(String userId, String language);
    List<MemoryItemDto> searchMemory(String query, String userId, String language);
    MemoryItemDto updateMemory(String memoryId, MemoryItemDto memoryDto);
}
