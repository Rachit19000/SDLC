package com.sdlc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service for GitHub API operations using a specific user's OAuth access token.
 * Provides repository listing, creation, and validation.
 * 
 * SECURITY: Never stores or logs access tokens.
 * Each method requires the caller to pass the user's access token.
 */
@Service
public class GitHubApiService {

    private static final Logger log = LoggerFactory.getLogger(GitHubApiService.class);
    private static final String GITHUB_API_BASE = "https://api.github.com";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Fetch the authenticated user's repositories from GitHub.
     * Returns only repos owned by the user (not org repos or forks unless owned).
     *
     * @param accessToken User's GitHub OAuth access token
     * @return List of repo info maps
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchUserRepos(String accessToken) {
        log.info("Fetching user repositories from GitHub...");

        List<Map<String, Object>> allRepos = new ArrayList<>();
        int page = 1;
        int perPage = 100;

        try {
            while (true) {
                String url = String.format("%s/user/repos?per_page=%d&page=%d&sort=updated&direction=desc&affiliation=owner",
                        GITHUB_API_BASE, perPage, page);

                HttpHeaders headers = createHeaders(accessToken);
                HttpEntity<Void> request = new HttpEntity<>(headers);

                ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, request, List.class);
                List<Map<String, Object>> repos = response.getBody();

                if (repos == null || repos.isEmpty()) {
                    break;
                }

                // Map each repo to a clean DTO with only the fields the frontend needs
                for (Map<String, Object> repo : repos) {
                    Map<String, Object> repoInfo = new LinkedHashMap<>();
                    repoInfo.put("id", repo.get("id"));
                    repoInfo.put("name", repo.get("name"));
                    repoInfo.put("full_name", repo.get("full_name"));
                    repoInfo.put("description", repo.get("description"));
                    repoInfo.put("html_url", repo.get("html_url"));
                    repoInfo.put("private", repo.get("private"));
                    repoInfo.put("language", repo.get("language"));
                    repoInfo.put("default_branch", repo.get("default_branch"));
                    repoInfo.put("updated_at", repo.get("updated_at"));
                    repoInfo.put("pushed_at", repo.get("pushed_at"));
                    repoInfo.put("stargazers_count", repo.get("stargazers_count"));
                    repoInfo.put("fork", repo.get("fork"));

                    // Extract owner info
                    Map<String, Object> owner = (Map<String, Object>) repo.get("owner");
                    if (owner != null) {
                        repoInfo.put("owner_login", owner.get("login"));
                        repoInfo.put("owner_avatar_url", owner.get("avatar_url"));
                    }

                    allRepos.add(repoInfo);
                }

                if (repos.size() < perPage) {
                    break; // Last page
                }
                page++;
            }

            log.info("Fetched {} repositories for user", allRepos.size());
            return allRepos;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                log.error("GitHub token invalid/expired ({}). User needs to re-authenticate.", e.getStatusCode().value());
                throw new RuntimeException("GitHub token expired or invalid. Please re-authenticate.");
            }
            log.error("GitHub API error fetching repos: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch repositories: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching user repos: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch repositories: " + e.getMessage());
        }
    }

    /**
     * Create a new GitHub repository under the authenticated user's account.
     *
     * @param accessToken User's GitHub OAuth access token
     * @param name Repository name
     * @param description Repository description
     * @param isPrivate Whether the repo should be private
     * @return Map with created repo info
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> createRepo(String accessToken, String name, String description, boolean isPrivate) {
        log.info("Creating new repository: {} (private: {})", name, isPrivate);

        String url = GITHUB_API_BASE + "/user/repos";
        HttpHeaders headers = createHeaders(accessToken);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("description", description != null ? description : "Created by SDLC Automation Platform");
        body.put("private", isPrivate);
        body.put("auto_init", true); // Initialize with README

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            Map<String, Object> repoData = response.getBody();

            if (repoData == null) {
                throw new RuntimeException("Empty response from GitHub when creating repository");
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", repoData.get("id"));
            result.put("name", repoData.get("name"));
            result.put("full_name", repoData.get("full_name"));
            result.put("description", repoData.get("description"));
            result.put("html_url", repoData.get("html_url"));
            result.put("private", repoData.get("private"));
            result.put("default_branch", repoData.get("default_branch"));

            Map<String, Object> owner = (Map<String, Object>) repoData.get("owner");
            if (owner != null) {
                result.put("owner_login", owner.get("login"));
            }

            log.info("Repository '{}' created successfully: {}", name, result.get("html_url"));
            return result;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                throw new RuntimeException("GitHub token expired or invalid. Please re-authenticate.");
            }
            if (e.getStatusCode().value() == 422) {
                // 422 usually means repo already exists
                log.warn("Repository '{}' may already exist: {}", name, e.getResponseBodyAsString());
                throw new RuntimeException("Repository '" + name + "' already exists or name is invalid.");
            }
            log.error("GitHub API error creating repo: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to create repository: " + e.getMessage());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating repo: {}", e.getMessage());
            throw new RuntimeException("Failed to create repository: " + e.getMessage());
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
}
