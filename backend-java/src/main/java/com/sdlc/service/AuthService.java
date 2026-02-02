package com.sdlc.service;

import com.sdlc.dto.LoginRequest;
import com.sdlc.dto.LoginResponse;
import com.sdlc.model.User;
import com.sdlc.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        
        User user = userRepository.findByEmailAndPassword(request.getEmail(), request.getPassword())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        
        // Generate mock token (In production, use JWT)
        String token = "mock_token_" + user.getId() + "_" + System.currentTimeMillis();
        
        log.info("User logged in successfully: {}", user.getEmail());
        
        return LoginResponse.builder()
                .token(token)
                .user(LoginResponse.UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .build())
                .build();
    }
    
    public User validateToken(String token) {
        if (token == null || !token.startsWith("mock_token_")) {
            throw new RuntimeException("Invalid token");
        }
        
        // Extract user ID from token
        // Token format: "mock_token_{userId}_{timestamp}"
        // Remove "mock_token_" prefix (11 characters)
        String tokenWithoutPrefix = token.substring(11);
        
        // Find the last underscore (which separates userId from timestamp)
        int lastUnderscoreIndex = tokenWithoutPrefix.lastIndexOf("_");
        if (lastUnderscoreIndex == -1) {
            throw new RuntimeException("Invalid token format");
        }
        
        // Extract userId (everything before the last underscore)
        String userId = tokenWithoutPrefix.substring(0, lastUnderscoreIndex);
        
        log.debug("Validating token for user ID: {}", userId);
        
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found for ID: {}", userId);
                    return new RuntimeException("User not found");
                });
    }
}
