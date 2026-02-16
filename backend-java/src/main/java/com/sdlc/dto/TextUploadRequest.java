package com.sdlc.dto;

import jakarta.validation.constraints.NotBlank;

public class TextUploadRequest {
    @NotBlank(message = "Text is required")
    private String text;

    private String name;

    @NotBlank(message = "repoOwner is required")
    private String repoOwner;

    @NotBlank(message = "repoName is required")
    private String repoName;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRepoOwner() { return repoOwner; }
    public void setRepoOwner(String repoOwner) { this.repoOwner = repoOwner; }

    public String getRepoName() { return repoName; }
    public void setRepoName(String repoName) { this.repoName = repoName; }
}
