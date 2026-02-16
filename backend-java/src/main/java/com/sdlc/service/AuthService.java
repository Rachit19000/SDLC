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
    private final TokenStore tokenStore;

    public AuthService(UserRepository userRepository, TokenStore tokenStore) {
        this.userRepository = userRepository;
        this.tokenStore = tokenStore;
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

    /**
     * Validate a token and return the corresponding User.
     * Supports both:
     * - mock_token_* (legacy mock login)
     * - ghsession_* (GitHub OAuth sessions stored in TokenStore)
     */
    public User validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Authentication token required");
        }

        // Handle GitHub OAuth session tokens
        if (token.startsWith("ghsession_")) {
            TokenStore.TokenData tokenData = tokenStore.get(token);
            if (tokenData == null) {
                throw new RuntimeException("Invalid or expired session. Please re-authenticate.");
            }

            // Create a User object from the token data
            User user = new User();
            user.setId(tokenData.getUserId());
            user.setEmail(tokenData.getEmail());
            user.setName(tokenData.getName());
            return user;
        }

        // Handle legacy mock tokens
        if (token.startsWith("mock_token_")) {
            String tokenWithoutPrefix = token.substring(11);
            int lastUnderscoreIndex = tokenWithoutPrefix.lastIndexOf("_");
            if (lastUnderscoreIndex == -1) {
                throw new RuntimeException("Invalid token format");
            }

            String userId = tokenWithoutPrefix.substring(0, lastUnderscoreIndex);
            log.debug("Validating mock token for user ID: {}", userId);

            return userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("User not found for ID: {}", userId);
                        return new RuntimeException("User not found");
                    });
        }

        throw new RuntimeException("Invalid token");
    }
}
