package com.sdlc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class GitHubOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GitHubOAuthService.class);

    private static final String GITHUB_AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
    private static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String GITHUB_USER_URL = "https://api.github.com/user";
    private static final String GITHUB_USER_EMAILS_URL = "https://api.github.com/user/emails";

    @Value("${github.oauth.client-id}")
    private String clientId;

    @Value("${github.oauth.client-secret}")
    private String clientSecret;

    @Value("${github.oauth.redirect-uri}")
    private String redirectUri;

    @Value("${github.oauth.scopes}")
    private String scopes;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Build the GitHub OAuth authorization URL that the user should be redirected to.
     */
    public String buildAuthorizationUrl(String state) {
        return GITHUB_AUTHORIZE_URL
                + "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&scope=" + URLEncoder.encode(scopes, StandardCharsets.UTF_8)
                + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
    }

    /**
     * Exchange the authorization code received from GitHub for an access token.
     */
    @SuppressWarnings("unchecked")
    public String exchangeCodeForToken(String code) {
        log.info("Exchanging authorization code for access token...");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        Map<String, String> body = Map.of(
                "client_id", clientId,
                "client_secret", clientSecret,
                "code", code
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    GITHUB_TOKEN_URL, HttpMethod.POST, request, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || responseBody.containsKey("error")) {
                String error = responseBody != null ? (String) responseBody.get("error_description") : "Unknown error";
                log.error("GitHub token exchange failed: {}", error);
                throw new RuntimeException("Failed to exchange code for token: " + error);
            }

            String accessToken = (String) responseBody.get("access_token");
            if (accessToken == null || accessToken.isBlank()) {
                throw new RuntimeException("No access_token in GitHub response");
            }

            log.info("Successfully obtained access token from GitHub");
            return accessToken;

        } catch (Exception e) {
            log.error("Error exchanging code for token: {}", e.getMessage());
            throw new RuntimeException("GitHub token exchange failed: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch the authenticated user's profile from GitHub using the access token.
     * Returns a GitHubUserInfo containing all user details including the login (username)
     * and avatar URL.
     */
    @SuppressWarnings("unchecked")
    public GitHubUserInfo fetchGitHubUser(String accessToken) {
        log.info("Fetching GitHub user profile...");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("User-Agent", "SDLC-Automation-Platform");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> userResponse = restTemplate.exchange(
                    GITHUB_USER_URL, HttpMethod.GET, request, Map.class);

            Map<String, Object> userBody = userResponse.getBody();
            if (userBody == null || userBody.get("id") == null) {
                throw new RuntimeException("Invalid user response from GitHub");
            }

            String githubId = userBody.get("id").toString();
            String login = (String) userBody.get("login");
            String name = userBody.get("name") != null ? userBody.get("name").toString() : login;
            String email = userBody.get("email") != null ? userBody.get("email").toString() : null;
            String avatarUrl = userBody.get("avatar_url") != null ? userBody.get("avatar_url").toString() : "";

            // If email is null, try fetching from /user/emails endpoint
            if (email == null || email.isBlank()) {
                email = fetchPrimaryEmail(headers);
            }
            if (email == null || email.isBlank()) {
                email = login + "@users.noreply.github.com";
            }

            log.info("GitHub user authenticated:");
            log.info("  GitHub ID: {}", githubId);
            log.info("  Username: {}", login);
            log.info("  Name: {}", name);
            log.info("  Email: {}", email);

            return new GitHubUserInfo(githubId, login, name, email, avatarUrl);

        } catch (Exception e) {
            log.error("Error fetching GitHub user: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch GitHub user: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch the primary email from GitHub /user/emails endpoint.
     */
    @SuppressWarnings("unchecked")
    private String fetchPrimaryEmail(HttpHeaders headers) {
        try {
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<List> emailsResponse = restTemplate.exchange(
                    GITHUB_USER_EMAILS_URL, HttpMethod.GET, request, List.class);

            List<Map<String, Object>> emails = emailsResponse.getBody();
            if (emails != null) {
                for (Map<String, Object> emailEntry : emails) {
                    Boolean primary = (Boolean) emailEntry.get("primary");
                    Boolean verified = (Boolean) emailEntry.get("verified");
                    if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                        return (String) emailEntry.get("email");
                    }
                }
                for (Map<String, Object> emailEntry : emails) {
                    Boolean verified = (Boolean) emailEntry.get("verified");
                    if (Boolean.TRUE.equals(verified)) {
                        return (String) emailEntry.get("email");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch user emails from GitHub: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Holds the GitHub user's profile information.
     */
    public static class GitHubUserInfo {
        private final String githubId;
        private final String login; // GitHub username
        private final String name;
        private final String email;
        private final String avatarUrl;

        public GitHubUserInfo(String githubId, String login, String name, String email, String avatarUrl) {
            this.githubId = githubId;
            this.login = login;
            this.name = name;
            this.email = email;
            this.avatarUrl = avatarUrl;
        }

        public String getGithubId() { return githubId; }
        public String getLogin() { return login; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getAvatarUrl() { return avatarUrl; }
    }
}
