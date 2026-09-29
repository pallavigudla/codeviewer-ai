package com.codereviewagent.service;

import com.codereviewagent.dto.MemoryItemDto;
import com.codereviewagent.entity.User;
import com.codereviewagent.hindsight.HindsightCloudClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryServiceImpl implements MemoryService {

    private final HindsightCloudClient hindsightCloudClient;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    public MemoryItemDto saveMemory(MemoryItemDto memoryDto) {
        User user = authenticatedUserService.getCurrentUser();
        String userIdStr = user.getId().toString();
        String devName = (user.getFullName() != null && !user.getFullName().isBlank()) ? user.getFullName() : user.getUsername();

        String lang = memoryDto.getLanguage() != null ? memoryDto.getLanguage() : "General";
        String mistake = memoryDto.getMistake() != null ? memoryDto.getMistake().trim() : "";
        String suggestion = (memoryDto.getSuggestion() != null && !"N/A".equalsIgnoreCase(memoryDto.getSuggestion().trim()) && !memoryDto.getSuggestion().isBlank())
                ? memoryDto.getSuggestion().trim()
                : null;
        String improvement = (memoryDto.getImprovement() != null && !"N/A".equalsIgnoreCase(memoryDto.getImprovement().trim()) && !memoryDto.getImprovement().isBlank())
                ? memoryDto.getImprovement().trim()
                : null;

        if (mistake.isBlank()) {
            log.info("Empty mistake content. Skipping memory creation.");
            return null;
        }

        // Filter out broad claims about user identity
        if (mistake.contains("is a Python developer") || mistake.contains("is a developer who uses") || mistake.contains("is a Java developer")) {
            log.info("Filter out personal claim memory: {}", mistake);
            return null;
        }

        // Deduplication Check: Recall existing memories for user and check if identical issue already exists
        List<MemoryItemDto> existingMemories = hindsightCloudClient.recall(userIdStr, lang, mistake);
        boolean isDuplicate = existingMemories.stream().anyMatch(m ->
            m.getMistake() != null &&
            m.getMistake().trim().equalsIgnoreCase(mistake)
        );

        if (isDuplicate) {
            log.info("Duplicate memory rule detected for user [{}] in language [{}] with issue [{}]. Skipping duplicate save.",
                    userIdStr, lang, mistake);
            return existingMemories.stream()
                    .filter(m -> m.getMistake() != null && m.getMistake().trim().equalsIgnoreCase(mistake))
                    .findFirst()
                    .orElse(memoryDto);
        }

        // Perform Real Hindsight Cloud RETAIN
        boolean success = hindsightCloudClient.retain(userIdStr, devName, lang, mistake, suggestion, improvement);
        if (!success) {
            log.warn("Hindsight Cloud RETAIN failed for user [{}]", userIdStr);
        }

        return MemoryItemDto.builder()
                .id(UUID.randomUUID().toString())
                .userId(userIdStr)
                .developerName(devName)
                .language(lang)
                .mistake(mistake)
                .suggestion(suggestion)
                .improvement(improvement)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public List<MemoryItemDto> retrieveMemory(String userId, String language) {
        User user = authenticatedUserService.getCurrentUser();
        String authenticatedUserId = user.getId().toString();

        return hindsightCloudClient.recall(authenticatedUserId, language, null);
    }

    @Override
    public List<MemoryItemDto> searchMemory(String query, String userId, String language) {
        User user = authenticatedUserService.getCurrentUser();
        String authenticatedUserId = user.getId().toString();

        return hindsightCloudClient.recall(authenticatedUserId, language, query);
    }

    @Override
    public MemoryItemDto updateMemory(String memoryId, MemoryItemDto memoryDto) {
        User user = authenticatedUserService.getCurrentUser();
        String userIdStr = user.getId().toString();
        String devName = (user.getFullName() != null && !user.getFullName().isBlank()) ? user.getFullName() : user.getUsername();

        String lang = memoryDto.getLanguage() != null ? memoryDto.getLanguage() : "General";
        String mistake = memoryDto.getMistake() != null ? memoryDto.getMistake().trim() : "";
        String suggestion = (memoryDto.getSuggestion() != null && !"N/A".equalsIgnoreCase(memoryDto.getSuggestion().trim())) ? memoryDto.getSuggestion().trim() : null;
        String improvement = (memoryDto.getImprovement() != null && !"N/A".equalsIgnoreCase(memoryDto.getImprovement().trim())) ? memoryDto.getImprovement().trim() : null;

        boolean success = hindsightCloudClient.retain(userIdStr, devName, lang, mistake, suggestion, improvement);
        if (!success) {
            log.warn("Hindsight Cloud RETAIN failed during update for user [{}]", userIdStr);
        }

        return MemoryItemDto.builder()
                .id(memoryId != null ? memoryId : UUID.randomUUID().toString())
                .userId(userIdStr)
                .developerName(devName)
                .language(lang)
                .mistake(mistake)
                .suggestion(suggestion)
                .improvement(improvement)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
