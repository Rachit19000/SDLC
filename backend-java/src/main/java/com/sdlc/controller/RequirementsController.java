package com.sdlc.controller;

import com.sdlc.dto.ErrorResponse;
import com.sdlc.dto.TextUploadRequest;
import com.sdlc.dto.UploadResponse;
import com.sdlc.model.User;
import com.sdlc.service.AuthService;
import com.sdlc.service.DocumentParserService;
import com.sdlc.service.GitHubService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/requirements")
public class RequirementsController {
    
    private static final Logger log = LoggerFactory.getLogger(RequirementsController.class);
    private final AuthService authService;
    private final DocumentParserService documentParserService;
    private final GitHubService gitHubService;
    
    public RequirementsController(AuthService authService, 
                                  DocumentParserService documentParserService,
                                  GitHubService gitHubService) {
        this.authService = authService;
        this.documentParserService = documentParserService;
        this.gitHubService = gitHubService;
    }
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name) {
        
        log.info("\n=== File Upload Request Received ===");
        
        try {
            // Authenticate user
            User user = authenticateUser(authHeader);
            log.info("User authenticated: {}", user.getEmail());
            
            // Validate file
            if (file.isEmpty()) {
                log.error("ERROR: No file uploaded");
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("BAD_REQUEST", "No file uploaded"));
            }
            
            String fileName = file.getOriginalFilename();
            String mimeType = file.getContentType();
            long fileSize = file.getSize();
            
            log.info("File details:");
            log.info("  Name: {}", fileName);
            log.info("  Type: {}", mimeType);
            log.info("  Size: {} bytes", fileSize);
            
            // Check if file type is supported
            if (!documentParserService.isSupportedFileType(mimeType, fileName)) {
                log.error("ERROR: Unsupported file type");
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("BAD_REQUEST", 
                                "Unsupported file type: " + mimeType + ". Supported: PDF, DOCX, TXT"));
            }
            
            // Step 1: Parse document
            log.info("\n📄 Step 1: Parsing document...");
            DocumentParserService.ParseResult parseResult = documentParserService.parseDocument(file);
            String extractedText = parseResult.getText();
            Map<String, Object> metadata = parseResult.getMetadata();
            
            log.info("✅ Document parsed successfully");
            log.info("  Extracted text length: {} characters", extractedText.length());
            if (metadata.containsKey("pages")) {
                log.info("  Pages: {}", metadata.get("pages"));
            }
            
            // Step 2: Store in GitHub
            String jobId = "job_" + System.currentTimeMillis();
            String baseFileName = fileName.substring(0, fileName.lastIndexOf('.'));
            String textFileName = baseFileName + "_extracted.txt";
            String filePath = "requirements/" + user.getId() + "/" + jobId + "/" + textFileName;
            
            log.info("\n📤 Step 2: Uploading extracted text to GitHub...");
            log.info("  File path: {}", filePath);
            
            // Create commit message
            StringBuilder commitMessage = new StringBuilder("Add parsed requirement: ")
                    .append(fileName)
                    .append(" (Job: ")
                    .append(jobId)
                    .append(")");
            
            if (metadata.containsKey("pages")) {
                commitMessage.append(" - ").append(metadata.get("pages")).append(" pages");
            }
            commitMessage.append(" - ").append(extractedText.length()).append(" characters extracted");
            
            GitHubService.GitHubResult githubResult = gitHubService.createFileInGitHub(
                    user.getEmail(),
                    filePath,
                    extractedText,
                    commitMessage.toString()
            );
            
            log.info("✅ SUCCESS! Extracted text uploaded to GitHub");
            log.info("  GitHub URL: {}", githubResult.getFileUrl());
            log.info("===================================\n");
            
            return ResponseEntity.accepted().body(UploadResponse.builder()
                    .jobId(jobId)
                    .status("stored")
                    .message("File parsed and text uploaded successfully to GitHub")
                    .githubUrl(githubResult.getFileUrl())
                    .commitUrl(githubResult.getCommitUrl())
                    .filePath(filePath)
                    .fileName(textFileName)
                    .originalFileName(fileName)
                    .extractedTextLength(extractedText.length())
                    .metadata(metadata)
                    .build());
            
        } catch (IOException e) {
            log.error("\n❌ ERROR in file upload: {}", e.getMessage(), e);
            log.error("===================================\n");
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("INTERNAL_ERROR", 
                            "Failed to process file", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("\n❌ ERROR: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ErrorResponse.of("UNAUTHORIZED", e.getMessage()));
        }
    }
    
    @PostMapping("/paste")
    public ResponseEntity<?> uploadText(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody TextUploadRequest request) {
        
        log.info("\n=== Text Upload Request Received ===");
        
        try {
            // Authenticate user
            User user = authenticateUser(authHeader);
            log.info("User authenticated: {}", user.getEmail());
            log.info("Text length: {}", request.getText().length());
            
            // Generate job ID and file name
            String jobId = "job_" + System.currentTimeMillis();
            String fileName = (request.getName() != null && !request.getName().isEmpty())
                    ? request.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".txt"
                    : "requirement_" + jobId + ".txt";
            
            String filePath = "requirements/" + user.getId() + "/" + jobId + "/" + fileName;
            
            log.info("File path: {}", filePath);
            log.info("Attempting to upload to GitHub...");
            
            // Upload to GitHub
            String commitMessage = "Add requirement text: " + fileName + " (Job: " + jobId + ")";
            GitHubService.GitHubResult githubResult = gitHubService.createFileInGitHub(
                    user.getEmail(),
                    filePath,
                    request.getText(),
                    commitMessage
            );
            
            log.info("✅ SUCCESS! Text uploaded to GitHub");
            log.info("  GitHub URL: {}", githubResult.getFileUrl());
            log.info("===================================\n");
            
            return ResponseEntity.accepted().body(UploadResponse.builder()
                    .jobId(jobId)
                    .status("stored")
                    .message("Text uploaded successfully to GitHub")
                    .githubUrl(githubResult.getFileUrl())
                    .commitUrl(githubResult.getCommitUrl())
                    .filePath(filePath)
                    .fileName(fileName)
                    .build());
            
        } catch (IOException e) {
            log.error("\n❌ ERROR in text upload: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("INTERNAL_ERROR", 
                            "Failed to upload text to GitHub", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("\n❌ ERROR: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ErrorResponse.of("UNAUTHORIZED", e.getMessage()));
        }
    }
    
    private User authenticateUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Authentication token required");
        }
        
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        return authService.validateToken(token);
    }
}
