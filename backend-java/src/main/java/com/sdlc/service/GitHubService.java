package com.sdlc.service;

import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class GitHubService {
    
    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
    
    @Value("${github.token}")
    private String githubToken;
    
    @Value("${github.user.rachitjainemail@gmail.com.owner}")
    private String defaultOwner;
    
    @Value("${github.user.rachitjainemail@gmail.com.repo}")
    private String defaultRepo;
    
    @Value("${github.user.rachitjainemail@gmail.com.branch}")
    private String defaultBranch;
    
    private GitHub github;
    private final Map<String, RepoConfig> userRepos = new HashMap<>();
    
    @PostConstruct
    public void initialize() throws IOException {
        if (githubToken == null || githubToken.isEmpty()) {
            log.error("ERROR: GITHUB_TOKEN environment variable is not set!");
            log.error("Please set GITHUB_TOKEN environment variable or in application.properties");
            throw new IllegalStateException("GITHUB_TOKEN is required");
        }
        
        // Initialize GitHub API
        github = new GitHubBuilder().withOAuthToken(githubToken).build();
        
        // Map user emails to repositories
        userRepos.put("rachitjainemail@gmail.com", 
                new RepoConfig(defaultOwner, defaultRepo, defaultBranch));
        
        log.info("✅ GitHub service initialized");
    }
    
    public GitHubResult createFileInGitHub(String userEmail, String filePath, 
                                           String content, String commitMessage) throws IOException {
        log.info("📤 Uploading to GitHub: {}", filePath);
        
        RepoConfig config = userRepos.get(userEmail);
        if (config == null) {
            throw new IllegalArgumentException("No GitHub repository configured for user: " + userEmail);
        }
        
        try {
            // Get repository
            GHRepository repository = github.getRepository(config.owner + "/" + config.repo);
            
            // Encode content to base64
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            
            // Check if file exists
            GHContent existingFile = null;
            try {
                existingFile = repository.getFileContent(filePath, config.branch);
            } catch (IOException e) {
                // File doesn't exist, that's okay
            }
            
            // Create or update file
            GHContentBuilder contentBuilder = repository.createContent()
                    .content(contentBytes)
                    .path(filePath)
                    .message(commitMessage)
                    .branch(config.branch);
            
            if (existingFile != null) {
                contentBuilder.sha(existingFile.getSha());
            }
            
            GHContentUpdateResponse response = contentBuilder.commit();
            
            String fileUrl = "https://github.com/" + config.owner + "/" + config.repo + 
                             "/blob/" + config.branch + "/" + filePath;
            String commitUrl = response.getCommit().getHtmlUrl().toString();
            
            log.info("✅ File uploaded to GitHub: {}", fileUrl);
            
            return new GitHubResult(true, fileUrl, commitUrl, response.getContent().getSha());
            
        } catch (IOException e) {
            log.error("❌ GitHub API Error: {}", e.getMessage());
            throw new RuntimeException("Failed to create file in GitHub: " + e.getMessage(), e);
        }
    }
    
    public RepoConfig getUserRepo(String email) {
        return userRepos.get(email);
    }
    
    public static class RepoConfig {
        private final String owner;
        private final String repo;
        private final String branch;
        
        public RepoConfig(String owner, String repo, String branch) {
            this.owner = owner;
            this.repo = repo;
            this.branch = branch;
        }
        
        public String getOwner() { return owner; }
        public String getRepo() { return repo; }
        public String getBranch() { return branch; }
    }
    
    public static class GitHubResult {
        private final boolean success;
        private final String fileUrl;
        private final String commitUrl;
        private final String sha;
        
        public GitHubResult(boolean success, String fileUrl, String commitUrl, String sha) {
            this.success = success;
            this.fileUrl = fileUrl;
            this.commitUrl = commitUrl;
            this.sha = sha;
        }
        
        public boolean isSuccess() { return success; }
        public String getFileUrl() { return fileUrl; }
        public String getCommitUrl() { return commitUrl; }
        public String getSha() { return sha; }
    }
}
