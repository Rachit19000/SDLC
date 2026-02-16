package com.sdlc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store mapping session tokens to GitHub OAuth access tokens and user data.
 * SECURITY: GitHub access tokens are NEVER logged or exposed to the frontend.
 * In production, use Redis or an encrypted database.
 */
@Component
public class TokenStore {

    private static final Logger log = LoggerFactory.getLogger(TokenStore.class);
    private final ConcurrentHashMap<String, TokenData> store = new ConcurrentHashMap<>();

    public void store(String sessionToken, TokenData data) {
        store.put(sessionToken, data);
        log.info("Session stored for user: {} ({})", data.getName(), data.getGithubUsername());
    }

    public TokenData get(String sessionToken) {
        return store.get(sessionToken);
    }

    public void remove(String sessionToken) {
        store.remove(sessionToken);
    }

    public boolean exists(String sessionToken) {
        return store.containsKey(sessionToken);
    }

    public static class TokenData {
        private final String githubAccessToken;
        private final String userId;
        private final String email;
        private final String name;
        private final String githubUsername;
        private final String avatarUrl;

        public TokenData(String githubAccessToken, String userId, String email,
                         String name, String githubUsername, String avatarUrl) {
            this.githubAccessToken = githubAccessToken;
            this.userId = userId;
            this.email = email;
            this.name = name;
            this.githubUsername = githubUsername;
            this.avatarUrl = avatarUrl;
        }

        public String getGithubAccessToken() { return githubAccessToken; }
        public String getUserId() { return userId; }
        public String getEmail() { return email; }
        public String getName() { return name; }
        public String getGithubUsername() { return githubUsername; }
        public String getAvatarUrl() { return avatarUrl; }
    }
}
