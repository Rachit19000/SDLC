package com.sdlc.dto;

import java.util.Map;

public class UploadResponse {
    private String jobId;
    private String status;
    private String message;
    private String githubUrl;
    private String commitUrl;
    private String filePath;
    private String fileName;
    private String originalFileName;
    private Integer extractedTextLength;
    private Map<String, Object> metadata;
    
    public UploadResponse() {}
    
    public UploadResponse(String jobId, String status, String message, String githubUrl, 
                         String commitUrl, String filePath, String fileName, 
                         String originalFileName, Integer extractedTextLength, 
                         Map<String, Object> metadata) {
        this.jobId = jobId;
        this.status = status;
        this.message = message;
        this.githubUrl = githubUrl;
        this.commitUrl = commitUrl;
        this.filePath = filePath;
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.extractedTextLength = extractedTextLength;
        this.metadata = metadata;
    }
    
    // Getters and setters
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }
    
    public String getCommitUrl() { return commitUrl; }
    public void setCommitUrl(String commitUrl) { this.commitUrl = commitUrl; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    
    public Integer getExtractedTextLength() { return extractedTextLength; }
    public void setExtractedTextLength(Integer extractedTextLength) { this.extractedTextLength = extractedTextLength; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String jobId;
        private String status;
        private String message;
        private String githubUrl;
        private String commitUrl;
        private String filePath;
        private String fileName;
        private String originalFileName;
        private Integer extractedTextLength;
        private Map<String, Object> metadata;
        
        public Builder jobId(String jobId) { this.jobId = jobId; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder githubUrl(String githubUrl) { this.githubUrl = githubUrl; return this; }
        public Builder commitUrl(String commitUrl) { this.commitUrl = commitUrl; return this; }
        public Builder filePath(String filePath) { this.filePath = filePath; return this; }
        public Builder fileName(String fileName) { this.fileName = fileName; return this; }
        public Builder originalFileName(String originalFileName) { this.originalFileName = originalFileName; return this; }
        public Builder extractedTextLength(Integer extractedTextLength) { this.extractedTextLength = extractedTextLength; return this; }
        public Builder metadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }
        
        public UploadResponse build() {
            return new UploadResponse(jobId, status, message, githubUrl, commitUrl, 
                                    filePath, fileName, originalFileName, 
                                    extractedTextLength, metadata);
        }
    }
}
