package com.sdlc.mcp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

    @Value("${mcp.host.url}")
    private String mcpHostUrl;

    public String getMcpHostUrl() {
        return mcpHostUrl;
    }
}
