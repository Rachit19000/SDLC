package com.sdlc.mcp;

import java.util.Map;

public class McpExecuteResponse {

    private String status;
    private Map<String, Object> metadata;
    private String error;

    public McpExecuteResponse() {}

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
