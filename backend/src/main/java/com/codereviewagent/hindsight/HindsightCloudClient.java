package com.codereviewagent.hindsight;

import com.codereviewagent.dto.MemoryItemDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
public class HindsightCloudClient {

    @Value("${hindsight.api-key:}")
    private String apiKey;

    @Value("${hindsight.base-url:https://api.hindsight.vectorize.io}")
    private String baseUrl;

    @Value("${hindsight.bank-id:code-review-agent}")
    private String bankId;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public HindsightCloudClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Hindsight RETAIN: Persists concrete code review knowledge into Hindsight Cloud REST API
     * POST /v1/default/banks/{bank_id}/memories
     * Uses documented tags for user isolation: user:<authenticated-user-id>
     */
    public boolean retain(String userId, String developerName, String language, String mistake, String suggestion, String improvement) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("HINDSIGHT_API_KEY is not configured. Skipping remote Hindsight Cloud retain operation.");
            return false;
        }

        try {
            String cleanBaseUrl = baseUrl.replaceAll("/+$", "");
            String cleanBankId = bankId.trim();
            String url = String.format("%s/v1/default/banks/%s/memories", cleanBaseUrl, cleanBankId);

            String lang = language != null ? language : "General";
            String mist = mistake != null ? mistake.trim() : "";
            String sugg = (suggestion != null && !"N/A".equalsIgnoreCase(suggestion.trim())) ? suggestion.trim() : "";
            String impr = (improvement != null && !"N/A".equalsIgnoreCase(improvement.trim())) ? improvement.trim() : "";

            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append("Category: ").append(lang);
            if (!mist.isEmpty()) contentBuilder.append(" | Issue: ").append(mist);
            if (!sugg.isEmpty()) contentBuilder.append(" | Suggestion: ").append(sugg);
            if (!impr.isEmpty()) contentBuilder.append(" | Learning: ").append(impr);

            String contentText = contentBuilder.toString();
            List<String> tags = List.of("user:" + userId, "lang:" + lang);

            Map<String, Object> memoryContent = Map.of(
                    "content", contentText,
                    "tags", tags
            );

            Map<String, Object> requestBody = Map.of(
                    "items", List.of(memoryContent)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully retained memory for user [{}] in Hindsight Cloud bank [{}]", userId, cleanBankId);
                return true;
            } else {
                log.warn("Hindsight Cloud RETAIN returned status: {}", response.getStatusCode());
            }
        } catch (Exception ex) {
            log.error("Hindsight Cloud RETAIN operation encountered error: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Hindsight RECALL: Retrieves relevant memories for the authenticated user from Hindsight Cloud REST API
     * POST /v1/default/banks/{bank_id}/memories/recall
     * Uses documented tags for user isolation: user:<authenticated-user-id>
     */
    public List<MemoryItemDto> recall(String userId, String language, String query) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("HINDSIGHT_API_KEY is not configured. Skipping remote Hindsight Cloud recall.");
            return Collections.emptyList();
        }

        try {
            String cleanBaseUrl = baseUrl.replaceAll("/+$", "");
            String cleanBankId = bankId.trim();
            String url = String.format("%s/v1/default/banks/%s/memories/recall", cleanBaseUrl, cleanBankId);

            String searchQuery = (query != null && !query.isBlank()) ? query : (language != null ? language : "code review patterns");

            Map<String, Object> requestBody = Map.of(
                    "query", searchQuery,
                    "tags", List.of("user:" + userId),
                    "limit", 15
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode itemsNode = root.path("results");
                if (!itemsNode.isArray()) {
                    itemsNode = root.path("memories");
                }
                if (!itemsNode.isArray()) {
                    itemsNode = root.path("items");
                }

                List<MemoryItemDto> recalledList = new ArrayList<>();
                Set<String> seenIssues = new HashSet<>();

                if (itemsNode.isArray()) {
                    for (JsonNode item : itemsNode) {
                        String rawContent = "";
                        if (item.has("content") && !item.get("content").isNull()) {
                            rawContent = item.get("content").asText();
                        } else if (item.has("text") && !item.get("text").isNull()) {
                            rawContent = item.get("text").asText();
                        } else if (item.has("memory") && !item.get("memory").isNull()) {
                            rawContent = item.get("memory").asText();
                        } else if (item.has("body") && !item.get("body").isNull()) {
                            rawContent = item.get("body").asText();
                        } else {
                            rawContent = item.asText();
                        }
                        MemoryItemDto dto = parseMemoryContent(rawContent, userId, language);
                        if (dto != null && dto.getMistake() != null && !dto.getMistake().isBlank()) {
                            String dedupKey = (dto.getLanguage() + ":" + dto.getMistake()).toLowerCase(Locale.ROOT).trim();
                            if (!seenIssues.contains(dedupKey)) {
                                seenIssues.add(dedupKey);
                                recalledList.add(dto);
                            }
                        }
                    }
                }
                log.info("Hindsight Cloud RECALL retrieved {} deduplicated memories for user [{}]", recalledList.size(), userId);
                return recalledList;
            }
        } catch (Exception ex) {
            log.error("Hindsight Cloud RECALL operation encountered error: {}", ex.getMessage());
        }
        return Collections.emptyList();
    }

    private MemoryItemDto parseMemoryContent(String rawContent, String userId, String fallbackLanguage) {
        String devName = "Developer";
        String lang = fallbackLanguage != null ? fallbackLanguage : "General";
        String mistake = rawContent;
        String suggestion = null;
        String improvement = null;

        if (rawContent != null && rawContent.contains("|")) {
            String[] parts = rawContent.split("\\|");
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.startsWith("Category:")) {
                    lang = trimmed.substring("Category:".length()).trim();
                } else if (trimmed.startsWith("Language:")) {
                    lang = trimmed.substring("Language:".length()).trim();
                } else if (trimmed.startsWith("Issue:")) {
                    mistake = trimmed.substring("Issue:".length()).trim();
                } else if (trimmed.startsWith("Mistake:")) {
                    mistake = trimmed.substring("Mistake:".length()).trim();
                } else if (trimmed.startsWith("Suggestion:")) {
                    String val = trimmed.substring("Suggestion:".length()).trim();
                    if (!val.equalsIgnoreCase("N/A") && !val.isBlank() && !"Follow best practices".equalsIgnoreCase(val)) {
                        suggestion = val;
                    }
                } else if (trimmed.startsWith("Learning:")) {
                    String val = trimmed.substring("Learning:".length()).trim();
                    if (!val.equalsIgnoreCase("N/A") && !val.isBlank() && !"None".equalsIgnoreCase(val)) {
                        improvement = val;
                    }
                } else if (trimmed.startsWith("Improvement:")) {
                    String val = trimmed.substring("Improvement:".length()).trim();
                    if (!val.equalsIgnoreCase("N/A") && !val.isBlank() && !"None".equalsIgnoreCase(val)) {
                        improvement = val;
                    }
                }
            }
        }

        // Ignore old broad personal claim memories if recalled
        if (mistake != null && (mistake.contains("is a Python developer") || mistake.contains("is a developer who uses") || mistake.contains("is a Java developer"))) {
            return null;
        }

        return MemoryItemDto.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .developerName(devName)
                .language(lang)
                .mistake(mistake)
                .suggestion(suggestion)
                .improvement(improvement)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
