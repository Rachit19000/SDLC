package com.sdlc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Uploads files to ANY user's GitHub repository using their OAuth access token.
 * Uses the GitHub Contents API: https://docs.github.com/en/rest/repos/contents
 *
 * CRITICAL: No hardcoded owner/repo. Everything is per-user and per-request.
 * SECURITY: Never stores or logs the access token.
 */
@Service
public class UserGitHubUploadService {

    private static final Logger log = LoggerFactory.getLogger(UserGitHubUploadService.class);
    private static final String GITHUB_API_BASE = "https://api.github.com";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Upload a file to the user's specified GitHub repo.
     *
     * @param accessToken    User's GitHub OAuth access token
     * @param repoOwner      Repository owner (GitHub username)
     * @param repoName       Repository name
     * @param filePath       Path within the repo (e.g., "requirements/parsed.md")
     * @param content        File content as text
     * @param commitMessage  Git commit message
     * @return UploadResult with file URL and commit URL
     */
    public UploadResult uploadFile(String accessToken, String repoOwner, String repoName,
                                   String filePath, String content, String commitMessage) {

        log.info("Uploading file to GitHub: {}/{}/{}", repoOwner, repoName, filePath);

        String url = String.format("%s/repos/%s/%s/contents/%s",
                GITHUB_API_BASE, repoOwner, repoName, filePath);

        HttpHeaders headers = createHeaders(accessToken);

        // Check if file already exists (to get SHA for update)
        String existingSha = getFileSha(accessToken, repoOwner, repoName, filePath);

        Map<String, String> body = new HashMap<>();
        body.put("message", commitMessage);
        body.put("content", Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8)));
        if (existingSha != null) {
            body.put("sha", existingSha);
        }

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
            return parseUploadResponse(response.getBody());

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                log.error("GitHub authentication failed (status {}). Token may be expired.", e.getStatusCode().value());
                throw new RuntimeException("GitHub token expired or invalid. Please re-authenticate.");
            }

            log.error("GitHub API error (status {}): {}", e.getStatusCode(), e.getResponseBodyAsString());

            // Retry once
            try {
                log.info("Retrying upload...");
                Thread.sleep(1000);
                ResponseEntity<String> retryResponse = restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
                return parseUploadResponse(retryResponse.getBody());
            } catch (Exception retryEx) {
                log.error("Retry failed: {}", retryEx.getMessage());
                throw new RuntimeException("GitHub upload failed after retry: " + e.getMessage());
            }

        } catch (Exception e) {
            log.error("Failed to upload to GitHub: {}", e.getMessage());
            throw new RuntimeException("Failed to upload to GitHub: " + e.getMessage());
        }
    }

    /**
     * Parse the GitHub API response for a content create/update.
     */
    private UploadResult parseUploadResponse(String responseBody) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            String fileUrl = json.path("content").path("html_url").asText("");
            String sha = json.path("content").path("sha").asText("");
            String commitUrl = json.path("commit").path("html_url").asText("");

            log.info("File uploaded successfully to GitHub: {}", fileUrl);
            return new UploadResult(true, fileUrl, commitUrl, sha);
        } catch (Exception e) {
            log.warn("Could not parse upload response, but upload may have succeeded.");
            return new UploadResult(true, "", "", "");
        }
    }

    /**
     * Get the SHA of an existing file (needed for updates).
     */
    private String getFileSha(String accessToken, String owner, String repo, String filePath) {
        String url = String.format("%s/repos/%s/%s/contents/%s",
                GITHUB_API_BASE, owner, repo, filePath);
        HttpHeaders headers = createHeaders(accessToken);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            return json.path("sha").asText(null);
        } catch (Exception e) {
            // File doesn't exist — that's fine
            return null;
        }
    }

    /**
     * Create standard headers for GitHub API calls.
     */
    private HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("User-Agent", "SDLC-Automation-Platform");
        return headers;
    }

    /**
     * Result of a GitHub file upload.
     */
    public static class UploadResult {
        private final boolean success;
        private final String fileUrl;
        private final String commitUrl;
        private final String sha;

        public UploadResult(boolean success, String fileUrl, String commitUrl, String sha) {
            this.success = success;
            this.fileUrl = fileUrl;
            this.commitUrl = commitUrl;
            this.sha = sha;
        }

        public boolean isSuccess() { return success; }
        public String getFileUrl() { return fileUrl; }
        public String getCommitUrl() { return commitUrl; }
        public String getSha() { return sha; }
    }
}
