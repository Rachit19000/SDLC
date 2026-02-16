package com.sdlc.controller;

import com.sdlc.dto.ErrorResponse;
import com.sdlc.service.TokenStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Controller for GitHub repository operations.
 * All operations use the authenticated user's GitHub OAuth token.
 * 
 * SECURITY: Never exposes tokens. All operations are per-user.
 */
@RestController
@RequestMapping("/github")
public class GitHubController {

    private static final Logger log = LoggerFactory.getLogger(GitHubController.class);
    private static final String GITHUB_API_BASE = "https://api.github.com";

    private final TokenStore tokenStore;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GitHubController(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    /**
     * Fetch all repositories for the authenticated user.
     * GET /api/v1/github/repos
     */
    @GetMapping("/repos")
    public ResponseEntity<?> getUserRepos(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("Fetching user's GitHub repositories...");

        try {
            TokenStore.TokenData tokenData = extractAndValidateToken(authHeader);
            String accessToken = tokenData.getGithubAccessToken();
            String githubUsername = tokenData.getGithubUsername();

            // Fetch user's repos using their OAuth token
            String url = GITHUB_API_BASE + "/user/repos?per_page=100&sort=updated";
            HttpHeaders headers = createHeaders(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<JsonNode[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, JsonNode[].class);

            JsonNode[] repos = response.getBody();
            if (repos == null) {
                return ResponseEntity.ok(Map.of("repos", Collections.emptyList()));
            }

            // Transform to simplified repo list
            List<Map<String, Object>> repoList = new ArrayList<>();
            for (JsonNode repo : repos) {
                Map<String, Object> repoInfo = new HashMap<>();
                repoInfo.put("id", repo.path("id").asLong());
                repoInfo.put("name", repo.path("name").asText());
                repoInfo.put("fullName", repo.path("full_name").asText());
                repoInfo.put("description", repo.path("description").asText(""));
                repoInfo.put("isPrivate", repo.path("private").asBoolean());
                repoInfo.put("url", repo.path("html_url").asText());
                repoInfo.put("owner", repo.path("owner").path("login").asText());
                repoInfo.put("updatedAt", repo.path("updated_at").asText());
                repoList.add(repoInfo);
            }

            log.info("Fetched {} repositories for user: {}", repoList.size(), githubUsername);

            return ResponseEntity.ok(Map.of("repos", repoList));

        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("GitHub authentication failed: token expired or invalid");
            return ResponseEntity.status(401)
                    .body(ErrorResponse.of("UNAUTHORIZED",
                            "GitHub token expired. Please re-authenticate."));
        } catch (Exception e) {
            log.error("Failed to fetch repositories: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("INTERNAL_ERROR",
                            "Failed to fetch repositories: " + e.getMessage()));
        }
    }

    /**
     * Create a new repository for the authenticated user.
     * POST /api/v1/github/repos
     */
    @PostMapping("/repos")
    public ResponseEntity<?> createRepo(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> requestBody) {

        log.info("Creating new GitHub repository...");

        try {
            TokenStore.TokenData tokenData = extractAndValidateToken(authHeader);
            String accessToken = tokenData.getGithubAccessToken();
            String githubUsername = tokenData.getGithubUsername();

            String repoName = (String) requestBody.get("name");
            if (repoName == null || repoName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("BAD_REQUEST", "Repository name is required"));
            }

            String description = requestBody.containsKey("description")
                    ? (String) requestBody.get("description")
                    : "Created by SDLC Automation Platform";
            Boolean isPrivate = requestBody.containsKey("isPrivate")
                    ? (Boolean) requestBody.get("isPrivate")
                    : false;

            // Create repo using user's OAuth token
            String url = GITHUB_API_BASE + "/user/repos";
            HttpHeaders headers = createHeaders(accessToken);

            Map<String, Object> createRequest = new HashMap<>();
            createRequest.put("name", repoName.trim());
            createRequest.put("description", description);
            createRequest.put("private", isPrivate);
            createRequest.put("auto_init", true);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(createRequest, headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, JsonNode.class);

            JsonNode repo = response.getBody();
            if (repo == null) {
                throw new RuntimeException("GitHub API returned null response");
            }

            Map<String, Object> repoInfo = new HashMap<>();
            repoInfo.put("id", repo.path("id").asLong());
            repoInfo.put("name", repo.path("name").asText());
            repoInfo.put("fullName", repo.path("full_name").asText());
            repoInfo.put("description", repo.path("description").asText(""));
            repoInfo.put("isPrivate", repo.path("private").asBoolean());
            repoInfo.put("url", repo.path("html_url").asText());
            repoInfo.put("owner", repo.path("owner").path("login").asText());

            log.info("Created repository: {}/{}", githubUsername, repoName);

            return ResponseEntity.status(201).body(repoInfo);

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                log.error("GitHub authentication failed: token expired or invalid");
                return ResponseEntity.status(401)
                        .body(ErrorResponse.of("UNAUTHORIZED",
                                "GitHub token expired. Please re-authenticate."));
            } else if (e.getStatusCode().value() == 422) {
                // 422 = validation error (e.g., repo name already exists)
                String errorMsg = "Repository name already exists or is invalid";
                try {
                    JsonNode errorBody = objectMapper.readTree(e.getResponseBodyAsString());
                    if (errorBody.has("errors") && errorBody.get("errors").isArray()) {
                        JsonNode firstError = errorBody.get("errors").get(0);
                        if (firstError.has("message")) {
                            errorMsg = firstError.get("message").asText();
                        }
                    }
                } catch (Exception ignored) {
                }
                return ResponseEntity.status(422)
                        .body(ErrorResponse.of("VALIDATION_ERROR", errorMsg));
            }
            log.error("GitHub API error: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode().value())
                    .body(ErrorResponse.of("GITHUB_API_ERROR", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create repository: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("INTERNAL_ERROR",
                            "Failed to create repository: " + e.getMessage()));
        }
    }

    /**
     * Extract and validate the Bearer token from the Authorization header.
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
}
