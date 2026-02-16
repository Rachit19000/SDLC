package com.sdlc.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Orchestrates async document processing: parsing, progress tracking, and GitHub upload.
 * Uses the user's GitHub OAuth access token for all GitHub operations.
 * Uploads to the user's selected repository — never to a shared/default repo.
 *
 * SECURITY: Temp files are always cleaned up after processing.
 */
@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final JobService jobService;
    private final UserGitHubUploadService userGitHubUploadService;
    private final McpAgentService mcpAgentService;

    public DocumentIngestionService(JobService jobService,
                                    UserGitHubUploadService userGitHubUploadService,
                                    McpAgentService mcpAgentService) {
        this.jobService = jobService;
        this.userGitHubUploadService = userGitHubUploadService;
        this.mcpAgentService = mcpAgentService;
    }

    /**
     * Asynchronously process a document upload:
     * 1. Detect file type
     * 2. Parse content (PDF/DOCX/TXT)
     * 3. Upload parsed text to user's SELECTED GitHub repo
     * 4. Emit progress events throughout
     *
     * @param jobId            Async job ID
     * @param tempFilePath     Path to the temp file on disk
     * @param originalFileName Original file name
     * @param mimeType         File MIME type
     * @param githubAccessToken User's GitHub OAuth access token
     * @param repoOwner        Target repo owner (user's GitHub username)
     * @param repoName         Target repo name (user-selected)
     * @param userId           Internal user ID
     */
    @Async("documentProcessingExecutor")
    public void processDocumentAsync(String jobId, Path tempFilePath, String originalFileName,
                                     String mimeType, String githubAccessToken,
                                     String repoOwner, String repoName, String userId) {
        try {
            // Stage 1: Initializing (0%)
            jobService.updateProgress(jobId, 0, "Initializing");
            Thread.sleep(500); // Brief pause so frontend has time to connect SSE

            // Stage 2: Detecting file type (10%)
            jobService.updateProgress(jobId, 10, "Detecting file type");
            Thread.sleep(300);

            // Stage 3: Parsing document (25%)
            jobService.updateProgress(jobId, 25, "Parsing document");

            byte[] fileBytes = Files.readAllBytes(tempFilePath);
            String extractedText = parseFile(fileBytes, originalFileName, mimeType);

            if (extractedText == null || extractedText.trim().isEmpty()) {
                throw new RuntimeException("No text content could be extracted from the document");
            }

            extractedText = cleanText(extractedText);

            // Stage 4: Text extraction complete (50%)
            jobService.updateProgress(jobId, 50,
                    "Text extracted (" + extractedText.length() + " characters)");
            Thread.sleep(300);

            // Stage 5: Generating user stories via agent (60%)
            jobService.updateProgress(jobId, 60, "Generating user stories from requirements");
            String userStories = null;
            try {
                userStories = mcpAgentService.generateUserStories(extractedText, jobId);
                log.info("User stories generated successfully for job: {}", jobId);
                jobService.updateProgress(jobId, 70, "User stories generated (" + 
                    (userStories.length() > 100 ? userStories.substring(0, 100) + "..." : userStories.length() + " chars") + ")");
            } catch (Exception e) {
                log.warn("Failed to generate user stories for job {}: {}. Continuing with parsed text only.", 
                    jobId, e.getMessage());
                // Continue with just parsed text if agent fails
            }

            // Stage 6: Preparing for upload (75%)
            jobService.updateProgress(jobId, 75, "Preparing for GitHub upload");

            // Build file paths and commit message
            String baseFileName = originalFileName.contains(".")
                    ? originalFileName.substring(0, originalFileName.lastIndexOf('.'))
                    : originalFileName;
            String parsedFileName = baseFileName + "_parsed.md";
            String parsedFilePath = "requirements/" + jobId + "/" + parsedFileName;
            
            // Upload parsed text first
            String commitMessage = "Add parsed requirement: " + originalFileName
                    + " (Job: " + jobId + ") - " + extractedText.length() + " characters";

            // Stage 7: Uploading parsed text to GitHub (80%)
            jobService.updateProgress(jobId, 80, "Uploading parsed text to " + repoOwner + "/" + repoName);

            // Upload parsed text using user's OAuth token to their SELECTED repo
            UserGitHubUploadService.UploadResult parsedResult = userGitHubUploadService.uploadFile(
                    githubAccessToken, repoOwner, repoName, parsedFilePath, extractedText, commitMessage);

            // Upload user stories if generated
            if (userStories != null && !userStories.trim().isEmpty()) {
                jobService.updateProgress(jobId, 85, "Uploading user stories to GitHub");
                String storiesFileName = baseFileName + "_user_stories.md";
                String storiesFilePath = "requirements/" + jobId + "/" + storiesFileName;
                String storiesCommitMessage = "Add user stories: " + storiesFileName + " (Job: " + jobId + ")";
                
                try {
                    UserGitHubUploadService.UploadResult storiesResult = userGitHubUploadService.uploadFile(
                            githubAccessToken, repoOwner, repoName, storiesFilePath, userStories, storiesCommitMessage);
                    log.info("User stories uploaded to: {}", storiesResult.getFileUrl());
                } catch (Exception e) {
                    log.warn("Failed to upload user stories: {}", e.getMessage());
                    // Continue even if stories upload fails
                }
            }

            // Stage 8: Finalizing (95%)
            jobService.updateProgress(jobId, 95, "Finalizing");
            Thread.sleep(300);

            // Stage 9: Complete (100%)
            jobService.completeJob(jobId, parsedResult.getFileUrl());

        } catch (Exception e) {
            log.error("Document processing failed for job {}: {}", jobId, e.getMessage(), e);

            if (e.getMessage() != null && (e.getMessage().contains("token expired")
                    || e.getMessage().contains("token invalid")
                    || e.getMessage().contains("401"))) {
                jobService.failJob(jobId, "GitHub token expired. Please re-authenticate.");
            } else {
                jobService.failJob(jobId, e.getMessage() != null ? e.getMessage() : "Unknown error");
            }
        } finally {
            // SECURITY: Always clean up temp files
            try {
                Files.deleteIfExists(tempFilePath);
                log.debug("Cleaned up temp file: {}", tempFilePath);
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", tempFilePath);
            }
        }
    }

    /**
     * Asynchronously process a text paste upload:
     * 1. Clean and prepare the pasted text
     * 2. Generate user stories via MCP requirements agent
     * 3. Upload parsed text and user stories to the user's selected GitHub repo
     * 4. Emit progress events throughout
     */
    @Async("documentProcessingExecutor")
    public void processTextAsync(String jobId, String text, String name,
                                 String githubAccessToken, String repoOwner,
                                 String repoName, String userId) {
        try {
            // Stage 1: Initializing (0%)
            jobService.updateProgress(jobId, 0, "Initializing");
            Thread.sleep(500);

            // Stage 2: Processing text (15%)
            jobService.updateProgress(jobId, 15, "Processing pasted text");
            String cleanedText = cleanText(text);
            if (cleanedText.isEmpty()) {
                throw new RuntimeException("No text content found in pasted input");
            }
            Thread.sleep(300);

            // Stage 3: Text ready (30%)
            jobService.updateProgress(jobId, 30,
                    "Text processed (" + cleanedText.length() + " characters)");
            Thread.sleep(300);

            // Stage 4: Generating user stories via requirements agent (40%)
            jobService.updateProgress(jobId, 40, "Generating user stories from requirements");
            String userStories = null;
            try {
                userStories = mcpAgentService.generateUserStories(cleanedText, jobId);
                log.info("User stories generated successfully for job: {}", jobId);
                jobService.updateProgress(jobId, 60, "User stories generated (" +
                    (userStories.length() > 100 ? userStories.substring(0, 100) + "..." : userStories.length() + " chars") + ")");
            } catch (Exception e) {
                log.warn("Failed to generate user stories for job {}: {}. Continuing with parsed text only.",
                    jobId, e.getMessage());
                jobService.updateProgress(jobId, 60, "User story generation skipped (agent unavailable)");
                // Continue with just parsed text if agent fails
            }

            // Stage 5: Preparing for upload (65%)
            jobService.updateProgress(jobId, 65, "Preparing for GitHub upload");

            String baseFileName = (name != null && !name.isEmpty())
                    ? name.replaceAll("[^a-zA-Z0-9._\\-]", "_")
                    : "requirement_" + jobId;
            String parsedFileName = baseFileName + "_parsed.md";
            String parsedFilePath = "requirements/" + jobId + "/" + parsedFileName;
            String commitMessage = "Add requirement text: " + parsedFileName + " (Job: " + jobId + ") - " + cleanedText.length() + " characters";

            // Stage 6: Uploading parsed text to GitHub (75%)
            jobService.updateProgress(jobId, 75, "Uploading parsed text to " + repoOwner + "/" + repoName);

            UserGitHubUploadService.UploadResult parsedResult = userGitHubUploadService.uploadFile(
                    githubAccessToken, repoOwner, repoName, parsedFilePath, cleanedText, commitMessage);

            // Stage 7: Upload user stories if generated (85%)
            if (userStories != null && !userStories.trim().isEmpty()) {
                jobService.updateProgress(jobId, 85, "Uploading user stories to GitHub");
                String storiesFileName = baseFileName + "_user_stories.md";
                String storiesFilePath = "requirements/" + jobId + "/" + storiesFileName;
                String storiesCommitMessage = "Add user stories: " + storiesFileName + " (Job: " + jobId + ")";

                try {
                    UserGitHubUploadService.UploadResult storiesResult = userGitHubUploadService.uploadFile(
                            githubAccessToken, repoOwner, repoName, storiesFilePath, userStories, storiesCommitMessage);
                    log.info("User stories uploaded to: {}", storiesResult.getFileUrl());
                } catch (Exception e) {
                    log.warn("Failed to upload user stories: {}", e.getMessage());
                    // Continue even if stories upload fails
                }
            }

            // Stage 8: Finalizing (95%)
            jobService.updateProgress(jobId, 95, "Finalizing");
            Thread.sleep(300);

            // Stage 9: Complete (100%)
            jobService.completeJob(jobId, parsedResult.getFileUrl());

        } catch (Exception e) {
            log.error("Text processing failed for job {}: {}", jobId, e.getMessage(), e);

            if (e.getMessage() != null && (e.getMessage().contains("token expired")
                    || e.getMessage().contains("token invalid")
                    || e.getMessage().contains("401"))) {
                jobService.failJob(jobId, "GitHub token expired. Please re-authenticate.");
            } else {
                jobService.failJob(jobId, e.getMessage() != null ? e.getMessage() : "Unknown error");
            }
        }
    }

    /**
     * Parse a file based on its MIME type.
     */
    private String parseFile(byte[] fileBytes, String fileName, String mimeType) throws IOException {
        if ("application/pdf".equals(mimeType) || fileName.toLowerCase().endsWith(".pdf")) {
            return parsePdf(fileBytes);
        } else if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(mimeType)
                || fileName.toLowerCase().endsWith(".docx")) {
            return parseDocx(fileBytes);
        } else if ("text/plain".equals(mimeType) || fileName.toLowerCase().endsWith(".txt")) {
            return new String(fileBytes, StandardCharsets.UTF_8);
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + mimeType);
        }
    }

    private String parsePdf(byte[] data) throws IOException {
        log.info("Parsing PDF file...");
        try (PDDocument document = Loader.loadPDF(data)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.info("PDF parsed: {} pages, {} characters", document.getNumberOfPages(), text.length());
            return text;
        }
    }

    private String parseDocx(byte[] data) throws IOException {
        log.info("Parsing Word document...");
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             XWPFDocument document = new XWPFDocument(bais);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            log.info("Word document parsed: {} characters", text.length());
            return text;
        }
    }

    private String cleanText(String text) {
        if (text == null) return "";
        return text
                .replaceAll("\r\n", "\n")
                .replaceAll("\r", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }
}
