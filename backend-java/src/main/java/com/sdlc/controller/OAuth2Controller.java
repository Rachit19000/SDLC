package com.sdlc.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/oauth2")
public class OAuth2Controller {

    private static final Logger log = LoggerFactory.getLogger(OAuth2Controller.class);

    /**
     * Get current authenticated user info (OAuth2)
     */
    @GetMapping("/user")
    public Map<String, Object> getCurrentUser(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            log.warn("No authenticated OAuth2 user found");
            throw new RuntimeException("Not authenticated");
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", "user_github_" + principal.getAttribute("id"));
        userInfo.put("username", principal.getAttribute("login"));
        userInfo.put("name", principal.getAttribute("name"));
        userInfo.put("email", principal.getAttribute("email"));
        userInfo.put("avatarUrl", principal.getAttribute("avatar_url"));
        userInfo.put("provider", "github");

        String username = principal.getAttribute("login");
        log.info("Returning OAuth2 user info for: " + username);
        return userInfo;
    }

    /**
     * Initiate GitHub OAuth2 login
     * Frontend should redirect to this endpoint to start OAuth flow
     */
    @GetMapping("/authorize/github")
    public void authorizeGitHub() {
        // Spring Security will handle the redirect to GitHub
        log.info("GitHub OAuth2 authorization initiated");
    }

    /**
     * Health check for OAuth2 endpoints
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "OAuth2 endpoints are available");
        return response;
    }
}
