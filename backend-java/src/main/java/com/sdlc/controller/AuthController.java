package com.sdlc.controller;

import com.sdlc.service.GitHubOAuthService;
import com.sdlc.service.TokenStore;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final GitHubOAuthService gitHubOAuthService;
    private final TokenStore tokenStore;

    @Value("${github.oauth.frontend-url}")
    private String frontendUrl;

    // Simple state store to prevent CSRF (in production, use Redis or DB)
    private final ConcurrentHashMap<String, Long> stateStore = new ConcurrentHashMap<>();

    public AuthController(GitHubOAuthService gitHubOAuthService, TokenStore tokenStore) {
        this.gitHubOAuthService = gitHubOAuthService;
        this.tokenStore = tokenStore;
    }

    /**
     * Step 1: Initiate GitHub OAuth flow.
     * Redirects the user to GitHub's authorization page.
     * GET /api/v1/auth/github
     */
    @GetMapping("/github")
    public void initiateGitHubOAuth(HttpServletResponse response) throws IOException {
        String state = UUID.randomUUID().toString();
        stateStore.put(state, System.currentTimeMillis());

        // Clean up old states (older than 10 minutes)
        long cutoff = System.currentTimeMillis() - 600_000;
        stateStore.entrySet().removeIf(entry -> entry.getValue() < cutoff);

        String authorizationUrl = gitHubOAuthService.buildAuthorizationUrl(state);
        log.info("Redirecting user to GitHub OAuth: {}", authorizationUrl);
        response.sendRedirect(authorizationUrl);
    }

    /**
     * Step 2: Handle GitHub OAuth callback.
     * Exchanges code for access token, fetches user info, stores token securely,
     * and redirects to the frontend with a session token (NOT the GitHub access token).
     *
     * GET /api/v1/auth/github/callback?code=...&state=...
     */
    @GetMapping("/github/callback")
    public void handleGitHubCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription,
            HttpServletResponse response) throws IOException {

        // Handle error from GitHub (e.g., user denied access)
        if (error != null) {
            log.error("GitHub OAuth error: {} - {}", error, errorDescription);
            String redirectUrl = frontendUrl + "/oauth/callback?error="
                    + URLEncoder.encode(errorDescription != null ? errorDescription : error, StandardCharsets.UTF_8);
            response.sendRedirect(redirectUrl);
            return;
        }

        // Validate code and state
        if (code == null || code.isBlank()) {
            log.error("Missing authorization code in GitHub callback");
            response.sendRedirect(frontendUrl + "/oauth/callback?error=Missing+authorization+code");
            return;
        }

        if (state == null || !stateStore.containsKey(state)) {
            log.error("Invalid or missing state parameter - possible CSRF attack");
            response.sendRedirect(frontendUrl + "/oauth/callback?error=Invalid+state+parameter");
            return;
        }
        stateStore.remove(state);

        try {
            // Step A: Exchange code for GitHub access token
            String accessToken = gitHubOAuthService.exchangeCodeForToken(code);

            // Step B: Fetch user info from GitHub (includes avatarUrl)
            GitHubOAuthService.GitHubUserInfo userInfo = gitHubOAuthService.fetchGitHubUser(accessToken);

            // Step C: Create a session token (this is what the frontend stores)
            String userId = "user_github_" + userInfo.getGithubId();
            String sessionToken = "ghsession_" + userInfo.getGithubId() + "_" + System.currentTimeMillis();

            // Step D: Store mapping: sessionToken → {accessToken, user info}
            // SECURITY: The GitHub access token is NEVER sent to the frontend.
            tokenStore.store(sessionToken, new TokenStore.TokenData(
                    accessToken,
                    userId,
                    userInfo.getEmail(),
                    userInfo.getName(),
                    userInfo.getLogin(),
                    userInfo.getAvatarUrl()
            ));

            // Step E: Redirect to frontend with session token + user info
            String redirectUrl = frontendUrl + "/oauth/callback"
                    + "?token=" + URLEncoder.encode(sessionToken, StandardCharsets.UTF_8)
                    + "&name=" + URLEncoder.encode(userInfo.getName(), StandardCharsets.UTF_8)
                    + "&email=" + URLEncoder.encode(userInfo.getEmail(), StandardCharsets.UTF_8)
                    + "&id=" + URLEncoder.encode(userId, StandardCharsets.UTF_8)
                    + "&githubUsername=" + URLEncoder.encode(userInfo.getLogin(), StandardCharsets.UTF_8)
                    + "&avatarUrl=" + URLEncoder.encode(userInfo.getAvatarUrl(), StandardCharsets.UTF_8);

            log.info("GitHub OAuth successful for user: {} ({}). Redirecting to frontend.",
                    userInfo.getName(), userInfo.getLogin());
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("GitHub OAuth flow failed: {}", e.getMessage());
            String redirectUrl = frontendUrl + "/oauth/callback?error="
                    + URLEncoder.encode("Authentication failed: " + e.getMessage(), StandardCharsets.UTF_8);
            response.sendRedirect(redirectUrl);
        }
    }

    /**
     * Health check endpoint.
     * GET /api/v1/auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Backend API is running"));
    }
}
