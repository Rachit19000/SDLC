package com.sdlc.controller;

import com.sdlc.dto.ErrorResponse;
import com.sdlc.dto.LoginRequest;
import com.sdlc.dto.LoginResponse;
import com.sdlc.service.AuthService;
import com.sdlc.service.GitHubAuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final GitHubAuthService gitHubAuthService;
    
    public AuthController(AuthService authService, GitHubAuthService gitHubAuthService) {
        this.authService = authService;
        this.gitHubAuthService = gitHubAuthService;
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ErrorResponse.of("UNAUTHORIZED", "Invalid email or password"));
        }
    }
    
    @PostMapping("/github-login")
    public ResponseEntity<?> githubLogin(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            
            log.info("GitHub login attempt for username: {}", username);
            
            LoginResponse response = gitHubAuthService.authenticateWithGitHub(username, password);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("GitHub login failed: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ErrorResponse.of("UNAUTHORIZED", 
                        "GitHub authentication failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Backend API is running"));
    }
}
