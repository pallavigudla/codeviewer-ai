package com.codereviewagent.service;

import com.codereviewagent.ai.LanguageDetector;
import com.codereviewagent.dto.CodeIssueDto;
import com.codereviewagent.dto.CodeReviewRequestDto;
import com.codereviewagent.dto.CodeReviewResultDto;
import com.codereviewagent.dto.MemoryItemDto;
import com.codereviewagent.dto.ReviewMetricDto;
import com.codereviewagent.entity.*;
import com.codereviewagent.entity.enums.ReviewStatus;
import com.codereviewagent.entity.enums.SubmissionStatus;
import com.codereviewagent.exception.*;
import com.codereviewagent.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroqAiServiceImpl implements AiService {

    @Value("${groq.api-key:}")
    private String apiKey;

    @Value("${groq.api-url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    @Value("${groq.timeout-ms:15000}")
    private int timeoutMs;

    private final LanguageDetector languageDetector;
    private final CodeSubmissionRepository codeSubmissionRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewMetricRepository reviewMetricRepository;
    private final ProjectRepository projectRepository;
    private final MemoryService memoryService;
    private final AuthenticatedUserService authenticatedUserService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("java", "js", "html", "css", "py", "cpp", "cxx", "cc", "c", "sql");
    private static final long MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024; // 2MB

    @Override
    @Transactional
    public CodeReviewResultDto uploadAndReviewFile(MultipartFile file, UUID projectId, UUID authorId) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File upload cannot be empty.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidFileException("File size exceeds maximum allowed limit of 2MB (actual: " + (file.getSize() / 1024) + " KB)");
        }

        String filename = file.getOriginalFilename();
        String extension = "";
        if (filename != null && filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase(Locale.ROOT);
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileException("Unsupported file type '." + extension + "'. Supported types: .java, .js, .html, .css, .py, .cpp, .sql");
        }

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (content.trim().isEmpty()) {
                throw new InvalidFileException("Uploaded file contains empty code content.");
            }

            String detectedLanguage = languageDetector.detectLanguage(filename, content);

            CodeReviewRequestDto request = CodeReviewRequestDto.builder()
                    .codeContent(content)
                    .language(detectedLanguage)
                    .title("Upload: " + (filename != null ? filename : "Code File"))
                    .description("Uploaded file analysis for " + filename)
                    .projectId(projectId)
                    .build();

            return reviewCode(request);
        } catch (IOException e) {
            log.error("Failed to read uploaded file: {}", e.getMessage());
            throw new InvalidFileException("Could not read uploaded file: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public CodeReviewResultDto reviewCode(CodeReviewRequestDto request) {
        String code = request.getCodeContent();
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Code content cannot be empty for AI review.");
        }

        String language = (request.getLanguage() != null && !request.getLanguage().isBlank())
                ? request.getLanguage()
                : languageDetector.detectLanguage(null, code);

        User author = authenticatedUserService.getCurrentUser();
        Project project = getOrCreateUserProject(request.getProjectId(), author);

        // 1. PRE-REVIEW: Search Hindsight for relevant memories for the authenticated developer
        List<MemoryItemDto> developerMemories = memoryService.retrieveMemory(author.getId().toString(), language);
        log.info("Retrieved {} Hindsight memories for authenticated developer [{}] in language [{}] before Groq AI request",
                developerMemories.size(), author.getUsername(), language);

        // 2. Call Groq AI API dynamically
        JsonNode aiResponse = callGroqAi(code, language, author.getFullName() != null ? author.getFullName() : author.getUsername(), developerMemories);

        List<CodeIssueDto> bugs = parseIssueList(aiResponse, "bugs");
        List<CodeIssueDto> security = parseIssueList(aiResponse, "security");
        List<CodeIssueDto> performance = parseIssueList(aiResponse, "performance");
        List<CodeIssueDto> readability = parseIssueList(aiResponse, "readability");
        List<CodeIssueDto> naming = parseIssueList(aiResponse, "naming");
        List<CodeIssueDto> bestPractices = parseIssueList(aiResponse, "bestPractices");
        List<CodeIssueDto> complexity = parseIssueList(aiResponse, "complexity");
        List<CodeIssueDto> improvements = parseIssueList(aiResponse, "improvements");

        double overallScore = aiResponse.has("overallScore") ? aiResponse.get("overallScore").asDouble(9.5) : 9.5;
        overallScore = Math.min(10.0, Math.max(1.0, Math.round(overallScore * 10.0) / 10.0));
        String summary = aiResponse.has("summary") ? aiResponse.get("summary").asText() : "Code analysis completed.";

        int totalBugs = bugs.size();
        int totalSecurity = security.size();
        int totalPerformance = performance.size();
        int totalReadability = readability.size();
        int totalNaming = naming.size();
        int totalBestPractices = bestPractices.size();
        int totalComplexity = complexity.size();
        int totalImprovements = improvements.size();
        int totalIssues = totalBugs + totalSecurity + totalPerformance + totalReadability + totalNaming + totalBestPractices + totalComplexity;

        // Dynamic metrics calculation reflecting submitted code
        double securityScore = (totalSecurity == 0) ? 10.0 : Math.max(2.0, Math.round((10.0 - totalSecurity * 2.5) * 10.0) / 10.0);
        double performanceScore = (totalPerformance == 0) ? 10.0 : Math.max(3.0, Math.round((10.0 - totalPerformance * 2.0) * 10.0) / 10.0);
        double codeQualityScore = overallScore;

        ReviewStatus verdict = ReviewStatus.APPROVED;
        if (totalBugs > 0 || totalSecurity > 0 || overallScore < 6.0) {
            verdict = ReviewStatus.CHANGES_REQUESTED;
        } else if (overallScore < 8.5 && (totalPerformance > 0 || totalComplexity > 0)) {
            verdict = ReviewStatus.COMMENTED;
        } else {
            verdict = ReviewStatus.APPROVED;
        }

        String formattedFeedback = formatFeedbackComments(bugs, security, performance, readability, naming, bestPractices, complexity, improvements);

        // 3. TRANSACTIONAL PERSISTENCE to PostgreSQL
        CodeSubmission submission = CodeSubmission.builder()
                .project(project)
                .author(author)
                .title(request.getTitle() != null ? request.getTitle() : "Code Submission - " + language)
                .description(request.getDescription() != null ? request.getDescription() : "Automated code review submission")
                .language(language)
                .diffContent(code)
                .status(verdict == ReviewStatus.APPROVED ? SubmissionStatus.APPROVED : SubmissionStatus.CHANGES_REQUESTED)
                .build();
        submission = codeSubmissionRepository.save(submission);

        Review review = Review.builder()
                .codeSubmission(submission)
                .reviewer(author)
                .isAiGenerated(true)
                .summary(summary)
                .feedbackComments(formattedFeedback)
                .verdict(verdict)
                .build();
        review = reviewRepository.save(review);

        ReviewMetric metric = ReviewMetric.builder()
                .review(review)
                .codeQualityScore(codeQualityScore)
                .securityScore(securityScore)
                .performanceScore(performanceScore)
                .totalIssuesFound(totalIssues)
                .criticalIssues(totalBugs + totalSecurity)
                .warningIssues(totalPerformance + totalComplexity)
                .infoIssues(totalReadability + totalNaming + totalBestPractices + totalImprovements)
                .build();
        metric = reviewMetricRepository.save(metric);

        ReviewMetricDto metricDto = ReviewMetricDto.builder()
                .id(metric.getId())
                .reviewId(review.getId())
                .codeQualityScore(metric.getCodeQualityScore())
                .securityScore(metric.getSecurityScore())
                .performanceScore(metric.getPerformanceScore())
                .totalIssuesFound(metric.getTotalIssuesFound())
                .criticalIssues(metric.getCriticalIssues())
                .warningIssues(metric.getWarningIssues())
                .infoIssues(metric.getInfoIssues())
                .createdAt(metric.getCreatedAt())
                .build();

        // 4. POST-REVIEW: Extract concrete reusable code knowledge into Hindsight Memory
        savePostReviewMemories(author, language, bugs, security, performance, readability, naming, bestPractices, complexity, improvements);

        return CodeReviewResultDto.builder()
                .reviewId(review.getId())
                .submissionId(submission.getId())
                .detectedLanguage(language)
                .bugs(bugs)
                .security(security)
                .performance(performance)
                .readability(readability)
                .naming(naming)
                .bestPractices(bestPractices)
                .complexity(complexity)
                .improvements(improvements)
                .overallScore(overallScore)
                .summary(summary)
                .feedbackComments(formattedFeedback)
                .verdict(verdict)
                .metrics(metricDto)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private JsonNode callGroqAi(String code, String language, String developerName, List<MemoryItemDto> memories) {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("GROQ_API_KEY is not configured.");
            throw new IllegalStateException("AI review service is temporarily unavailable.");
        }

        StringBuilder memoryContext = new StringBuilder();
        if (memories != null && !memories.isEmpty()) {
            memoryContext.append("\n### Developer Hindsight Memory Context for ").append(developerName).append(":\n");
            for (MemoryItemDto m : memories) {
                memoryContext.append("- Reusable Pattern: ").append(m.getMistake() != null ? m.getMistake() : "N/A").append("\n");
                if (m.getSuggestion() != null && !m.getSuggestion().isBlank()) {
                    memoryContext.append("  Recommended Fix: ").append(m.getSuggestion()).append("\n");
                }
                if (m.getImprovement() != null && !m.getImprovement().isBlank()) {
                    memoryContext.append("  Learning: ").append(m.getImprovement()).append("\n");
                }
            }
        }

        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(timeoutMs);
            factory.setReadTimeout(timeoutMs);
            RestTemplate restTemplate = new RestTemplate(factory);

            String systemContent = """
                    You are an expert, precise AI code reviewer and compiler analysis engine.
                    Review the submitted %s code accurately according to these rules:
                    
                    1. UNDERSTAND FIRST: The 'summary' field MUST state what the code does and whether it is syntactically valid or contains errors.
                    2. DO NOT INVENT ERRORS OR FALSE BUGS:
                       - Python semicolons (e.g. 'a = int(input());') are NOT syntax errors! Python permits them. Place under 'readability' or 'bestPractices' as a LOW severity style issue. Return "bugs": [].
                       - Theoretical input exceptions (e.g. ValueError on int(input())) are NOT bugs. Place under 'improvements' as optional robustness enhancements. Return "bugs": [].
                       - Division by zero (e.g. 'a = 10; b = 0; print(a / b)') is a HIGH severity bug on line 3.
                       - Undefined variables (e.g. 'print(undefined_variable)') are HIGH severity bugs on line 1.
                    3. EXACT LINE-LEVEL ISSUES SCHEMA:
                       Every reported item in any array ('bugs', 'security', 'performance', 'readability', 'naming', 'bestPractices', 'complexity', 'improvements') MUST be a JSON object with this EXACT schema:
                       {
                         "severity": "HIGH|MEDIUM|LOW",
                         "line": 1,
                         "code": "<exact_problematic_line_or_expression>",
                         "problem": "<short_title_or_description_of_problem>",
                         "why": "<detailed_explanation_of_why_it_fails>",
                         "fix": "<how_to_fix_it>",
                         "correctedCode": "<exact_corrected_code_snippet>"
                       }
                       Use line = 0 if the line number cannot be reliably determined.
                    4. CATEGORIES:
                       - 'bugs': Real syntax errors, compile-time errors, or guaranteed runtime exceptions (e.g., division by zero, NameError/undefined variables, NullPointerException).
                       - 'security': Real security vulnerabilities (hardcoded credentials, SQL injection).
                       - 'performance': Performance bottlenecks.
                       - 'readability': Code style or formatting (e.g. unnecessary semicolons in Python).
                       - 'naming': Naming convention issues.
                       - 'bestPractices': Language idioms.
                       - 'complexity': High complexity issues.
                       - 'improvements': Optional robustness suggestions.
                    5. CLEAN CODE:
                       - If a category has no issue, return an empty array [].
                       - If code is syntactically valid and bug-free, return "bugs": [], "security": [], and overallScore >= 9.0.
                    """.formatted(language);

            String prompt = """
                    Review this %s code carefully.
                    %s
                    
                    Return ONLY valid JSON matching this schema:
                    {
                      "summary": "The code is syntactically valid...",
                      "bugs": [
                        {
                          "severity": "HIGH",
                          "line": 3,
                          "code": "print(a / b)",
                          "problem": "Division by zero.",
                          "why": "b is 0, causing ZeroDivisionError at runtime.",
                          "fix": "Check b before division.",
                          "correctedCode": "if b != 0:\\n    print(a / b)"
                        }
                      ],
                      "security": [],
                      "performance": [],
                      "readability": [],
                      "naming": [],
                      "bestPractices": [],
                      "complexity": [],
                      "improvements": [],
                      "overallScore": 9.5
                    }
                    
                    Code to review:
                    %s
                    """.formatted(language, memoryContext.toString(), code);

            Map<String, Object> systemMessage = Map.of("role", "system", "content", systemContent);
            Map<String, Object> userMessage = Map.of("role", "user", "content", prompt);

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(systemMessage, userMessage),
                    "temperature", 0.1,
                    "response_format", Map.of("type", "json_object")
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.path("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    String contentStr = choices.get(0).path("message").path("content").asText();
                    JsonNode jsonNode = objectMapper.readTree(contentStr);

                    if (!jsonNode.has("bugs") || !jsonNode.has("security") || !jsonNode.has("overallScore") || !jsonNode.has("summary")) {
                        log.error("Groq AI response missing required JSON schema fields: {}", contentStr);
                        throw new IllegalStateException("Groq AI response did not match required JSON schema.");
                    }
                    return jsonNode;
                }
            }
        } catch (Exception ex) {
            log.error("Groq AI API request failed: {}", ex.getMessage());
            throw new IllegalStateException("AI review service is temporarily unavailable.");
        }

        throw new IllegalStateException("AI review service is temporarily unavailable.");
    }

    private List<CodeIssueDto> parseIssueList(JsonNode root, String fieldName) {
        List<CodeIssueDto> result = new ArrayList<>();
        if (root != null && root.has(fieldName) && root.get(fieldName).isArray()) {
            for (JsonNode item : root.get(fieldName)) {
                if (item.isObject()) {
                    String severity = item.has("severity") ? item.get("severity").asText("MEDIUM").toUpperCase(Locale.ROOT) : "MEDIUM";
                    int line = (item.has("line") && !item.get("line").isNull()) ? item.get("line").asInt(0) : 0;
                    String code = item.has("code") ? item.get("code").asText("") : "";
                    String problem = item.has("problem") ? item.get("problem").asText("") : "";
                    String why = item.has("why") ? item.get("why").asText("") : "";
                    String fix = item.has("fix") ? item.get("fix").asText("") : "";
                    String correctedCode = item.has("correctedCode") ? item.get("correctedCode").asText("") : "";

                    result.add(CodeIssueDto.builder()
                            .severity(severity)
                            .line(line)
                            .code(code)
                            .problem(problem)
                            .why(why)
                            .fix(fix)
                            .correctedCode(correctedCode)
                            .build());
                } else if (item.isTextual()) {
                    String text = item.asText();
                    result.add(CodeIssueDto.builder()
                            .severity("LOW")
                            .line(0)
                            .code("")
                            .problem(text)
                            .why("")
                            .fix("")
                            .correctedCode("")
                            .build());
                }
            }
        }
        return result;
    }

    private void savePostReviewMemories(User author, String language, List<CodeIssueDto> bugs, List<CodeIssueDto> security,
                                       List<CodeIssueDto> performance, List<CodeIssueDto> readability, List<CodeIssueDto> naming,
                                       List<CodeIssueDto> bestPractices, List<CodeIssueDto> complexity, List<CodeIssueDto> improvements) {
        List<CodeIssueDto> allIssues = new ArrayList<>();
        if (bugs != null) allIssues.addAll(bugs);
        if (security != null) allIssues.addAll(security);
        if (performance != null) allIssues.addAll(performance);
        if (readability != null) allIssues.addAll(readability);
        if (naming != null) allIssues.addAll(naming);
        if (bestPractices != null) allIssues.addAll(bestPractices);
        if (complexity != null) allIssues.addAll(complexity);
        if (improvements != null) allIssues.addAll(improvements);

        if (allIssues.isEmpty()) {
            log.info("No code issues found in review for language [{}]. Skipping Hindsight memory creation.", language);
            return;
        }

        // Limit memory generation per review to top 3 concrete issues to maintain high quality
        int limit = Math.min(3, allIssues.size());
        for (int i = 0; i < limit; i++) {
            CodeIssueDto issue = allIssues.get(i);
            if (issue == null || issue.getProblem() == null || issue.getProblem().isBlank()) {
                continue;
            }

            String lineStr = (issue.getLine() != null && issue.getLine() > 0) ? " on line " + issue.getLine() : "";
            String problem = issue.getProblem().trim() + lineStr;
            String fix = (issue.getFix() != null && !issue.getFix().isBlank()) ? issue.getFix().trim() : null;
            String why = (issue.getWhy() != null && !issue.getWhy().isBlank()) ? issue.getWhy().trim() : null;

            MemoryItemDto memDto = MemoryItemDto.builder()
                    .userId(author.getId().toString())
                    .developerName(author.getFullName() != null ? author.getFullName() : author.getUsername())
                    .language(language != null ? language : "General")
                    .mistake(problem)
                    .suggestion(fix)
                    .improvement(why)
                    .timestamp(LocalDateTime.now())
                    .build();

            try {
                memoryService.saveMemory(memDto);
            } catch (Exception ex) {
                log.error("Failed to save post-review memory to Hindsight store: {}", ex.getMessage());
            }
        }
    }

    private String formatFeedbackComments(List<CodeIssueDto> bugs, List<CodeIssueDto> security, List<CodeIssueDto> performance,
                                           List<CodeIssueDto> readability, List<CodeIssueDto> naming, List<CodeIssueDto> bestPractices,
                                           List<CodeIssueDto> complexity, List<CodeIssueDto> improvements) {
        StringBuilder sb = new StringBuilder();
        appendSection(sb, "Actual Errors & Bugs", bugs);
        appendSection(sb, "Security Issues", security);
        appendSection(sb, "Performance Issues", performance);
        appendSection(sb, "Readability & Style", readability);
        appendSection(sb, "Naming Conventions", naming);
        appendSection(sb, "Best Practices", bestPractices);
        appendSection(sb, "Complexity Findings", complexity);
        appendSection(sb, "Optional Improvements", improvements);
        return sb.toString();
    }

    private void appendSection(StringBuilder sb, String title, List<CodeIssueDto> items) {
        if (items != null && !items.isEmpty()) {
            sb.append("### ").append(title).append("\n");
            for (CodeIssueDto item : items) {
                String lineStr = (item.getLine() != null && item.getLine() > 0) ? " (Line " + item.getLine() + ")" : "";
                sb.append("- [").append(item.getSeverity() != null ? item.getSeverity() : "LOW").append("]").append(lineStr).append(": ")
                  .append(item.getProblem() != null ? item.getProblem() : "").append("\n");
                if (item.getCode() != null && !item.getCode().isBlank()) {
                    sb.append("  Code: `").append(item.getCode()).append("`\n");
                }
                if (item.getWhy() != null && !item.getWhy().isBlank()) {
                    sb.append("  Why: ").append(item.getWhy()).append("\n");
                }
                if (item.getFix() != null && !item.getFix().isBlank()) {
                    sb.append("  Fix: ").append(item.getFix()).append("\n");
                }
                if (item.getCorrectedCode() != null && !item.getCorrectedCode().isBlank()) {
                    sb.append("  Corrected Code:\n```\n").append(item.getCorrectedCode()).append("\n```\n");
                }
                sb.append("\n");
            }
        }
    }

    private Project getOrCreateUserProject(UUID projectId, User author) {
        if (projectId != null) {
            Optional<Project> proj = projectRepository.findById(projectId);
            if (proj.isPresent() && proj.get().getOwner() != null && proj.get().getOwner().getId().equals(author.getId())) {
                return proj.get();
            }
        }

        List<Project> userProjects = projectRepository.findByOwnerId(author.getId());
        if (!userProjects.isEmpty()) {
            return userProjects.get(0);
        }

        Project defaultProject = Project.builder()
                .name("Code Review Project")
                .repositoryUrl("https://github.com/codereviewagent/user-project")
                .defaultBranch("main")
                .description("Default individual developer project for AI code analysis")
                .owner(author)
                .build();
        return projectRepository.save(defaultProject);
    }
}
