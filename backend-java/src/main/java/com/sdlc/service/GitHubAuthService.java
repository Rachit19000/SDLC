package com.sdlc.service;

import com.sdlc.dto.LoginResponse;
import com.sdlc.dto.LoginResponse.UserDto;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class GitHubAuthService {

    private static final Logger log = LoggerFactory.getLogger(GitHubAuthService.class);

    /**
     * Authenticate user with GitHub using username/email and password/token
     * Supports both username and email address for login
     * Uses Basic Authentication or OAuth Token to verify credentials with GitHub API
     */
    public LoginResponse authenticateWithGitHub(String usernameOrEmail, String password) {
        try {
            log.info("Attempting GitHub authentication for: {}", usernameOrEmail);
            
            // Determine if input is email or username
            boolean isEmail = usernameOrEmail.contains("@");
            log.info("Login identifier type: {}", isEmail ? "email" : "username");
            
            // GitHub Basic Authentication using username/email and password
            // Note: GitHub deprecated password auth in 2021, but we'll try Basic Auth
            // If it fails, we'll check if it's a token
            GitHub github;
            
            try {
                // Try Basic Authentication (username:password or email:password)
                // GitHub API accepts both username and email for authentication
                github = new GitHubBuilder()
                        .withPassword(usernameOrEmail, password)
                        .build();
            } catch (Exception e) {
                log.warn("Basic auth failed, trying OAuth token format: {}", e.getMessage());
                // If password auth fails, check if it's actually a token (starts with ghp_)
                if (password.startsWith("ghp_") || password.startsWith("github_pat_")) {
                    github = new GitHubBuilder()
                            .withOAuthToken(password)
                            .build();
                } else {
                    throw new RuntimeException("GitHub password authentication is no longer supported. " +
                            "Please use a Personal Access Token instead. " +
                            "Get one at: https://github.com/settings/tokens");
                }
            }
            
            // Verify authentication by getting user info
            if (!github.isCredentialValid()) {
                log.error("Invalid GitHub credentials for: {}", usernameOrEmail);
                throw new RuntimeException("Invalid GitHub username/email or password. " +
                        "Note: GitHub no longer supports password authentication. " +
                        "Please use a Personal Access Token at: https://github.com/settings/tokens");
            }
            
            // Get authenticated user info
            GHUser ghUser = github.getMyself();
            
            // Get user information
            String githubLogin = ghUser.getLogin();
            String githubEmail = ghUser.getEmail() != null ? ghUser.getEmail() : "";
            
            // Verify the username/email matches
            boolean matches = false;
            if (isEmail) {
                // If user provided email, check if it matches GitHub email
                matches = githubEmail.equalsIgnoreCase(usernameOrEmail) || 
                         githubLogin.equalsIgnoreCase(usernameOrEmail);
                log.info("Email login - GitHub email: {}, GitHub username: {}, Provided: {}", 
                        githubEmail, githubLogin, usernameOrEmail);
            } else {
                // If user provided username, check if it matches GitHub username
                matches = githubLogin.equalsIgnoreCase(usernameOrEmail);
                log.info("Username login - GitHub username: {}, Provided: {}", 
                        githubLogin, usernameOrEmail);
            }
            
            if (!matches) {
                log.warn("Identifier mismatch: provided={}, GitHub username={}, GitHub email={}", 
                        usernameOrEmail, githubLogin, githubEmail);
                // Still allow login if token is valid, just log the mismatch
            }
            
            // Get user information from GitHub
            String githubId = String.valueOf(ghUser.getId());
            String name = ghUser.getName() != null ? ghUser.getName() : githubLogin;
            String email = ghUser.getEmail() != null ? ghUser.getEmail() : githubLogin + "@github.local";
            
            log.info("GitHub authentication successful:");
            log.info("  GitHub ID: {}", githubId);
            log.info("  Username: {}", githubLogin);
            log.info("  Name: {}", name);
            log.info("  Email: {}", email);
            
            // Create user DTO
            UserDto userDto = new UserDto(
                "user_github_" + githubId,
                email,
                name
            );
            
            // Generate token (in production, use JWT)
            String token = "github_token_" + githubId + "_" + System.currentTimeMillis();
            
            log.info("Generated token for GitHub user: {}", githubLogin);
            
            return new LoginResponse(token, userDto);
            
        } catch (IOException e) {
            log.error("GitHub API error: {}", e.getMessage());
            throw new RuntimeException("Failed to authenticate with GitHub: " + e.getMessage());
        } catch (Exception e) {
            log.error("GitHub authentication error: {}", e.getMessage());
            throw new RuntimeException("GitHub authentication failed: " + e.getMessage());
        }
    }
    
    /**
     * Validate if a GitHub token is valid
     */
    public boolean validateGitHubToken(String token) {
        try {
            GitHub github = new GitHubBuilder()
                    .withOAuthToken(token)
                    .build();
            return github.isCredentialValid();
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}
