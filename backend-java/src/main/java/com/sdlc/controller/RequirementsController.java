package com.sdlc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdlc.dto.ErrorResponse;
import com.sdlc.dto.TextUploadRequest;
import com.sdlc.model.Job;
import com.sdlc.service.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for the human-in-the-loop requirements workflow.
 *
 * NEW MULTI-STEP FLOW:
 * 1. POST /parse                → Parse file or text, return parsed text for user review
 * 2. POST /save-to-github       → Save user-edited text to GitHub
 * 3. POST /generate-requirements → Call user-req-agent, return FR/NFR/AC for review
 * 4. POST /generate-stories     → Call user-stories-agent, return user stories for review
 * 5. POST /generate-tech-specs  → Call tech-specs-agent, return tech spec for review
 *
 * Legacy endpoints (/upload, /paste) are kept for backward compatibility.
 *
 * SECURITY: Uses the authenticated user's GitHub OAuth access token (from TokenStore).
 */
@RestController
@RequestMapping("/requirements")
public class RequirementsController {

    private static final Logger log = LoggerFactory.getLogger(RequirementsController.class);
    private final TokenStore tokenStore;
    private final JobService jobService;
    private final DocumentIngestionService documentIngestionService;
    private final DocumentParserService documentParserService;
    private final UserGitHubUploadService userGitHubUploadService;
    private final McpAgentService mcpAgentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RequirementsController(TokenStore tokenStore,
                                  JobService jobService,
                                  DocumentIngestionService documentIngestionService,
                                  DocumentParserService documentParserService,
                                  UserGitHubUploadService userGitHubUploadService,
                                  McpAgentService mcpAgentService) {
        this.tokenStore = tokenStore;
        this.jobService = jobService;
        this.documentIngestionService = documentIngestionService;
        this.documentParserService = documentParserService;
        this.userGitHubUploadService = userGitHubUploadService;
        this.mcpAgentService = mcpAgentService;
    }

    // =====================================================================
    //  Step 1 — Parse file or text, return parsed text for user review
    // =====================================================================

    /**
     * Parse an uploaded file (PDF, DOCX, TXT) and return the extracted text
     * for the user to review/edit. Does NOT upload to GitHub.
     *
     * POST /api/v1/requirements/parse
     */
    @PostMapping("/parse")
    public ResponseEntity<?> parseFile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam("file") MultipartFile file) {

        log.info("\n=== Parse File Request Received ===");

        try {
            extractAndValidateToken(authHeader);

            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("BAD_REQUEST", "No file uploaded"));
            }

            String fileName = file.getOriginalFilename();
            String mimeType = file.getContentType();
            log.info("Parsing file: Name={}, Type={}, Size={} bytes", fileName, mimeType, file.getSize());

            if (!documentParserService.isSupportedFileType(mimeType, fileName)) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("BAD_REQUEST",
                                "Unsupported file type: " + mimeType + ". Supported: PDF, DOCX, TXT"));
            }

            // Parse the document synchronously
            DocumentParserService.ParseResult parseResult = documentParserService.parseDocument(file);

            Map<String, Object> response = new HashMap<>();
            response.put("parsedText", parseResult.getText());
            response.put("fileName", fileName);
            response.put("characterCount", parseResult.getText().length());
            response.put("metadata", parseResult.getMetadata());

            log.info("File parsed successfully: {} characters", parseResult.getText().length());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("ERROR parsing file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("PARSE_ERROR",
                            "Failed to parse the document", e.getMessage()));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Authentication")) {
                return ResponseEntity.status(401)
                        .body(ErrorResponse.of("UNAUTHORIZED", e.getMessage()));
            }
            log.error("ERROR: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("PARSE_ERROR", e.getMessage()));
        }
    }

    // =====================================================================
    //  Step 2 — Save user-edited text to GitHub
    // =====================================================================

    /**
     * Save user-reviewed/edited text to GitHub.
     *
     * POST /api/v1/requirements/save-to-github
     */
    @PostMapping("/save-to-github")
    public ResponseEntity<?> saveToGitHub(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> request) {

        log.info("\n=== Save to GitHub Request Received ===");

        try {
            TokenStore.TokenData tokenData = extractAndValidateToken(authHeader);

            String text = request.get("text");
            String fileName = request.get("fileName");
            String repoOwner = request.get("repoOwner");
            String repoName = request.get("repoName");
            String fileType = request.getOrDefault("fileType", "parsed");

            if (text == null || text.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("BAD_REQUEST", "Text content is required"));
            }
            if (repoOwner == null || repoOwner.isBlank() || repoName == null || repoName.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("BAD_REQUEST", "repoOwner and repoName are required"));
            }

            // Generate a unique job/folder ID
            String jobId = UUID.randomUUID().toString().substring(0, 8);

            String baseFileName = (fileName != null && !fileName.isBlank())
                    ? fileName.replaceAll("[^a-zA-Z0-9._\\-]", "_")
                    : "requirement";
            if (baseFileName.contains(".")) {
                baseFileName = baseFileName.substring(0, baseFileName.lastIndexOf('.'));
            }

            String suffix;
            String artifactLabel;
            switch (fileType) {
                case "requirements":
                    suffix = "_requirements.md";
                    artifactLabel = "structured requirements";
                    break;
                case "user_stories":
                    suffix = "_user_stories.md";
                    artifactLabel = "user stories";
                    break;
                case "tech_spec":
                    suffix = "_tech_spec.md";
                    artifactLabel = "tech specification";
                    break;
                default:
                    suffix = "_parsed.md";
                    artifactLabel = "parsed requirement";
                    break;
            }
            String targetFileName = baseFileName + suffix;
            String filePath = "requirements/" + jobId + "/" + targetFileName;
            String commitMessage = "Add " + artifactLabel + ": " + targetFileName + " (Job: " + jobId + ")";

            log.info("Uploading {} to {}/{}/{}", fileType, repoOwner, repoName, filePath);

            UserGitHubUploadService.UploadResult result = userGitHubUploadService.uploadFile(
                    tokenData.getGithubAccessToken(), repoOwner, repoName, filePath, text, commitMessage);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fileUrl", result.getFileUrl());
            response.put("commitUrl", result.getCommitUrl());
            response.put("filePath", filePath);
            response.put("jobId", jobId);

            log.info("File saved to GitHub: {}", result.getFileUrl());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            if (e.getMessage() != null && (e.getMessage().contains("Authentication")
                    || e.getMessage().contains("expired") || e.getMessage().contains("Invalid"))) {
                return ResponseEntity.status(401)
                        .body(ErrorResponse.of("UNAUTHORIZED", e.getMessage()));
            }
            log.error("Save to GitHub failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("UPLOAD_ERROR",
                            "Failed to save to GitHub", e.getMessage()));
        }
    }

    // =====================================================================
    //  Step 3 — Generate structured requirements (FR, NFR, AC)
    // =====================================================================

    /**
     * Call the user-req-agent to generate structured requirements (FR, NFR, AC)
     * from user-reviewed requirement text. Returns requirements for review.
     *
     * POST /api/v1/requirements/generate-requirements
     */
    @PostMapping("/generate-requirements")
    public ResponseEntity<?> generateRequirements(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> request) {

        log.info("\n=== Generate Requirements Request Received ===");

        try {
            extractAndValidateToken(authHeader);

            String requirementText = request.get("text");
            if (requirementText == null || requirementText.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("BAD_REQUEST", "Requirement text is required"));
            }

            String jobId = request.getOrDefault("jobId",
                    UUID.randomUUID().toString().substring(0, 8));

            log.info("Generating requirements for {} characters of text (job: {})",
                    requirementText.length(), jobId);

            // Call the MCP user-req-agent (synchronous)
            Map<String, Object> agentResult = mcpAgentService.generateRequirements(requirementText, jobId);

            Map<String, Object> response = new HashMap<>();
            response.put("jobId", jobId);

            // Extract requirements data
            if (agentResult.containsKey("requirements")) {
                response.put("requirements", agentResult.get("requirements"));
            }
            if (agentResult.containsKey("requirements_text")) {
                response.put("requirementsText", agentResult.get("requirements_text"));
            }
            if (agentResult.containsKey("functional_requirements_count")) {
                response.put("functionalRequirementsCount", agentResult.get("functional_requirements_count"));
            }
            if (agentResult.containsKey("nonfunctional_requirements_count")) {
                response.put("nonfunctionalRequirementsCount", agentResult.get("nonfunctional_requirements_count"));
            }
            if (agentResult.containsKey("acceptance_criteria_count")) {
                response.put("acceptanceCriteriaCount", agentResult.get("acceptance_criteria_count"));
            }

            // Convert requirements object to JSON string for passing to user-stories-agent
            if (agentResult.containsKey("requirements")) {
                try {
                    String reqJson = objectMapper.writeValueAsString(agentResult.get("requirements"));
                    response.put("requirementsJson", reqJson);
                } catch (Exception e) {
                    log.warn("Failed to serialize requirements to JSON: {}", e.getMessage());
                }
            }

            log.info("Requirements generated: FR={}, NFR={}, AC={}",
                    agentResult.getOrDefault("functional_requirements_count", "?"),
                    agentResult.getOrDefault("nonfunctional_requirements_count", "?"),
                    agentResult.getOrDefault("acceptance_criteria_count", "?"));

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            if (e.getMessage() != null && (e.getMessage().contains("Authentication")
                    || e.getMessage().contains("expired") || e.getMessage().contains("Invalid"))) {
                return ResponseEntity.status(401)
                        .body(ErrorResponse.of("UNAUTHORIZED", e.getMessage()));
            }
            log.error("Requirements generation failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("AGENT_ERROR",
                            "Failed to generate requirements", e.getMessage()));
        }
    }

    // =====================================================================
    //  Step 4 — Generate user stories from structured requirements
    // =====================================================================

    /**
     * Call the user-stories-agent to generate sprint-planned user stories
     * from structured requirements JSON. Returns the user stories for review.
     *
     * POST /api/v1/requirements/generate-stories
     */
    @PostMapping("/generate-stories")
    public ResponseEntity<?> generateStories(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> request) {

        log.info("\n=== Generate User Stories Request Received ===");

        try {
            extractAndValidateToken(authHeader);

            // Accept either requirementsJson (from step 3) or raw text (backward compat)
            String requirementsJson = request.get("requirementsJson");
            String requirementText = request.get("text");

            if ((requirementsJson == null || requirementsJson.isBlank())
                    && (requirementText == null || requirementText.isBlank())) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("BAD_REQUEST",
                                "Either requirementsJson or text is required"));
            }

            String jobId = request.getOrDefault("jobId",
                    UUID.randomUUID().toString().substring(0, 8));

            String userStories;

            if (requirementsJson != null && !requirementsJson.isBlank()) {
                // New flow: use structured requirements JSON directly
                log.info("Generating user stories from {} chars of requirements JSON (job: {})",
                        requirementsJson.length(), jobId);
                userStories = mcpAgentService.generateUserStories(requirementsJson, jobId);
            } else {
                // Backward compat: generate requirements first, then stories
                log.info("Generating user stories from {} chars of raw text (job: {})",
                        requirementText.length(), jobId);
                Map<String, Object> reqResult = mcpAgentService.generateRequirements(requirementText, jobId);
                String reqJson;
                if (reqResult.containsKey("requirements")) {
                    reqJson = objectMapper.writeValueAsString(reqResult.get("requirements"));
                } else {
                    reqJson = requirementText; // fallback
                }
                userStories = mcpAgentService.generateUserStories(reqJson, jobId);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("userStories", userStories);
            response.put("characterCount", userStories.length());
            response.put("jobId", jobId);

            log.info("User stories generated: {} characters", userStories.length());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            if (e.getMessage() != null && (e.getMessage().contains("Authentication")
                    || e.getMessage().contains("expired") || e.getMessage().contains("Invalid"))) {
                return ResponseEntity.status(401)
                        .body(ErrorResponse.of("UNAUTHORIZED", e.getMessage()));
            }
            log.error("User story generation failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("AGENT_ERROR",
                            "Failed to generate user stories", e.getMessage()));
        } catch (Exception e) {
            log.error("User story generation error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("AGENT_ERROR",
                            "Failed to generate user stories", e.getMessage()));
        }
    }

    // =====================================================================
    //  Step 5 — Generate tech specs from requirements + user stories
    // =====================================================================

    /**
     * Call the tech-specs agent to generate a technical specification from
     * user-reviewed requirement text and user stories. Returns the tech spec for review.
     *
     * POST /api/v1/requirements/generate-tech-specs
     */
    @PostMapping("/generate-tech-specs")
    public ResponseEntity<?> generateTechSpecs(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> request) {

        log.info("\n=== Generate Tech Specs Request Received ===");

        try {
            extractAndValidateToken(authHeader);

            String requirementText = request.get("text");
            String userStories = request.get("userStories");

            if (requirementText == null || requirementText.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("BAD_REQUEST", "Requirement text is required"));
            }

            String jobId = request.getOrDefault("jobId",
                    UUID.randomUUID().toString().substring(0, 8));

            log.info("Generating tech specs for {} chars of requirement text + {} chars of user stories (job: {})",
                    requirementText.length(),
                    userStories != null ? userStories.length() : 0,
                    jobId);

            // Call the MCP tech-specs agent (synchronous)
            String techSpec = mcpAgentService.generateTechSpecs(
                    requirementText,
                    userStories != null ? userStories : "",
                    jobId);

            Map<String, Object> response = new HashMap<>();
            response.put("techSpec", techSpec);
            response.put("characterCount", techSpec.length());
            response.put("jobId", jobId);

            log.info("Tech spec generated: {} characters", techSpec.length());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            if (e.getMessage() != null && (e.getMessage().contains("Authentication")
                    || e.getMessage().contains("expired") || e.getMessage().contains("Invalid"))) {
                return ResponseEntity.status(401)
                        .body(ErrorResponse.of("UNAUTHORIZED", e.getMessage()));
            }
            log.error("Tech spec generation failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("AGENT_ERROR",
                            "Failed to generate tech specification", e.getMessage()));
        }
    }

    // =====================================================================
    //  LEGACY: Original async upload endpoints (kept for compatibility)
    // =====================================================================

    /**
     * Upload a document file (PDF, DOCX, TXT) to a user-selected GitHub repo.
     * Returns immediately with a jobId. Processing happens asynchronously.
     *
     * POST /api/v1/requirements/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam("file") MultipartFile file,
            @RequestParam("repoOwner") String repoOwner,
            @RequestParam("repoName") String repoName,
            @RequestParam(value = "name", required = false) String name) {

        log.info("\n=== File Upload Request Received ===");

        try {
            // Authenticate and get token data
            TokenStore.TokenData tokenData = extractAndValidateToken(authHeader);
            log.info("User authenticated: {} ({})", tokenData.getName(), tokenData.getGithubUsername());
            log.info("Target repo: {}/{}", repoOwner, repoName);

            // Validate repo params
            if (repoOwner == null || repoOwner.isBlank() || repoName == null || repoName.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("BAD_REQUEST", "repoOwner and repoName are required"));
            }

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("BAD_REQUEST", "No file uploaded"));
            }

            String fileName = file.getOriginalFilename();
            String mimeType = file.getContentType();
            log.info("File details: Name={}, Type={}, Size={} bytes", fileName, mimeType, file.getSize());

            // Check if file type is supported
            if (!documentParserService.isSupportedFileType(mimeType, fileName)) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("BAD_REQUEST",
                                "Unsupported file type: " + mimeType + ". Supported: PDF, DOCX, TXT"));
            }

            // Save file to temp location
            Path tempFile = Files.createTempFile("sdlc_upload_", "_" + fileName);
            file.transferTo(tempFile);

            // Create async job
            Job job = jobService.createJob(fileName);
            log.info("Created async job: {} for file: {} → {}/{}", job.getJobId(), fileName, repoOwner, repoName);

            // Start async processing (non-blocking) — uploads to user's selected repo
            documentIngestionService.processDocumentAsync(
                    job.getJobId(),
                    tempFile,
                    fileName,
                    mimeType,
                    tokenData.getGithubAccessToken(),
                    repoOwner,
                    repoName,
                    tokenData.getUserId()
            );

            log.info("Async processing started for job: {}", job.getJobId());

            return ResponseEntity.accepted().body(Map.of(
                    "jobId", job.getJobId(),
                    "status", "PROCESSING",
                    "message", "Document upload accepted. Processing started.",
                    "targetRepo", repoOwner + "/" + repoName
            ));

        } catch (IOException e) {
            log.error("ERROR in file upload: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("INTERNAL_ERROR",
                            "Failed to process file upload", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("AUTH ERROR: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ErrorResponse.of("UNAUTHORIZED", e.getMessage()));
        }
    }

    /**
     * Upload pasted text requirements to a user-selected GitHub repo.
     * Returns immediately with a jobId. Processing happens asynchronously.
     *
     * POST /api/v1/requirements/paste
     */
    @PostMapping("/paste")
    public ResponseEntity<?> uploadText(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody TextUploadRequest request) {

        log.info("\n=== Text Upload Request Received ===");

        try {
            TokenStore.TokenData tokenData = extractAndValidateToken(authHeader);
            log.info("User authenticated: {} ({})", tokenData.getName(), tokenData.getGithubUsername());

            // Validate repo params
            if (request.getRepoOwner() == null || request.getRepoOwner().isBlank()
                    || request.getRepoName() == null || request.getRepoName().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("BAD_REQUEST", "repoOwner and repoName are required"));
            }

            log.info("Target repo: {}/{}", request.getRepoOwner(), request.getRepoName());
            log.info("Text length: {} characters", request.getText().length());

            // Create async job
            String jobName = request.getName() != null ? request.getName() : "pasted_text";
            Job job = jobService.createJob(jobName);
            log.info("Created async job: {} for text → {}/{}", job.getJobId(),
                    request.getRepoOwner(), request.getRepoName());

            // Start async processing (non-blocking)
            documentIngestionService.processTextAsync(
                    job.getJobId(),
                    request.getText(),
                    request.getName(),
                    tokenData.getGithubAccessToken(),
                    request.getRepoOwner(),
                    request.getRepoName(),
                    tokenData.getUserId()
            );

            return ResponseEntity.accepted().body(Map.of(
                    "jobId", job.getJobId(),
                    "status", "PROCESSING",
                    "message", "Text upload accepted. Processing started.",
                    "targetRepo", request.getRepoOwner() + "/" + request.getRepoName()
            ));

        } catch (RuntimeException e) {
            log.error("AUTH ERROR: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ErrorResponse.of("UNAUTHORIZED", e.getMessage()));
        }
    }

    /**
     * Extract the Bearer token from the Authorization header
     * and validate it against the TokenStore.
     */
    private TokenStore.TokenData extractAndValidateToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Authentication token required. Please sign in with GitHub.");
        }

        String token = authHeader.substring(7);
        TokenStore.TokenData tokenData = tokenStore.get(token);

        if (tokenData == null) {
            throw new RuntimeException("Invalid or expired session. Please re-authenticate with GitHub.");
        }

        if (tokenData.getGithubAccessToken() == null || tokenData.getGithubAccessToken().isBlank()) {
            throw new RuntimeException("GitHub access token not available. Please re-authenticate.");
        }

        return tokenData;
    }
}
