package com.sdlc.controller;

import com.sdlc.dto.ErrorResponse;
import com.sdlc.dto.TextUploadRequest;
import com.sdlc.model.Job;
import com.sdlc.service.DocumentIngestionService;
import com.sdlc.service.DocumentParserService;
import com.sdlc.service.JobService;
import com.sdlc.service.TokenStore;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Controller for uploading requirements documents.
 *
 * All uploads are processed ASYNCHRONOUSLY and uploaded to the USER'S SELECTED REPO:
 * 1. POST /upload or /paste → returns immediately with {jobId, status: "PROCESSING"}
 * 2. Frontend connects to SSE at /api/v1/jobs/{jobId}/progress for real-time updates
 * 3. Backend parses document, uploads to user's SELECTED GitHub repo using their OAuth token
 * 4. On completion, SSE emits COMPLETED with githubFileUrl
 *
 * SECURITY: Uses the authenticated user's GitHub OAuth access token (from TokenStore).
 * CRITICAL: repoOwner and repoName are required — no default/shared repos.
 */
@RestController
@RequestMapping("/requirements")
public class RequirementsController {

    private static final Logger log = LoggerFactory.getLogger(RequirementsController.class);
    private final TokenStore tokenStore;
    private final JobService jobService;
    private final DocumentIngestionService documentIngestionService;
    private final DocumentParserService documentParserService;

    public RequirementsController(TokenStore tokenStore,
                                  JobService jobService,
                                  DocumentIngestionService documentIngestionService,
                                  DocumentParserService documentParserService) {
        this.tokenStore = tokenStore;
        this.jobService = jobService;
        this.documentIngestionService = documentIngestionService;
        this.documentParserService = documentParserService;
    }

    /**
     * Upload a document file (PDF, DOCX, TXT) to a user-selected GitHub repo.
     * Returns immediately with a jobId. Processing happens asynchronously.
     *
     * POST /api/v1/requirements/upload
     * Params: file, repoOwner, repoName
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
