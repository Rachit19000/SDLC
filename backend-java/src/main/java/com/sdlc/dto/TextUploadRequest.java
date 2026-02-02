package com.sdlc.dto;

import jakarta.validation.constraints.NotBlank;

public class TextUploadRequest {
    @NotBlank(message = "Text is required")
    private String text;
    
    private String name;
    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
