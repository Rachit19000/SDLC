package com.sdlc.service;

import com.sdlc.mcp.MCPExecuteRequest;
import com.sdlc.mcp.McpExecuteResponse;
import com.sdlc.mcp.McpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;
import java.util.Map;

/**
 * Service to execute agents via MCP (Model Context Protocol) host.
 * Calls agents to generate artifacts like user stories, WBS, etc.
 */
@Service
public class McpAgentService {

    private static final Logger log = LoggerFactory.getLogger(McpAgentService.class);
    
    private final McpConfig mcpConfig;
    private final RestTemplate restTemplate;

    public McpAgentService(McpConfig mcpConfig) {
        this.mcpConfig = mcpConfig;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Execute an agent to generate user stories from parsed requirements.
     * 
     * @param parsedText The parsed requirement text
     * @param jobId The job ID for tracking
     * @return Generated user stories as string
     */
    public String generateUserStories(String parsedText, String jobId) {
        log.info("Calling user story agent for job: {}", jobId);
        
        try {
            // Prepare arguments for the agent
            Map<String, Object> arguments = new HashMap<>();
            arguments.put("requirement_text", parsedText);
            arguments.put("job_id", jobId);
            
            // Create MCP execute request
            MCPExecuteRequest request = new MCPExecuteRequest("requirements-agent", arguments);
            
            // Call MCP host
            String mcpHostUrl = mcpConfig.getMcpHostUrl();
            if (mcpHostUrl == null || mcpHostUrl.isEmpty()) {
                throw new RuntimeException("MCP host URL not configured. Please set mcp.host.url in application.properties");
            }
            
            String executeUrl = mcpHostUrl + "/execute";
            log.info("Calling MCP host at: {}", executeUrl);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<MCPExecuteRequest> httpEntity = new HttpEntity<>(request, headers);
            
            ResponseEntity<McpExecuteResponse> response = restTemplate.exchange(
                    executeUrl,
                    HttpMethod.POST,
                    httpEntity,
                    McpExecuteResponse.class
            );
            
            McpExecuteResponse mcpResponse = response.getBody();
            if (mcpResponse == null) {
                throw new RuntimeException("Empty response from MCP host");
            }
            
            if (!"success".equalsIgnoreCase(mcpResponse.getStatus())) {
                String errorDetail = mcpResponse.getError() != null ? mcpResponse.getError() : mcpResponse.getStatus();
                throw new RuntimeException("Agent execution failed: " + errorDetail);
            }
            
            // Extract user stories from metadata
            Map<String, Object> metadata = mcpResponse.getMetadata();
            if (metadata != null && metadata.containsKey("user_stories")) {
                Object stories = metadata.get("user_stories");
                return stories instanceof String ? (String) stories : stories.toString();
            } else if (metadata != null && metadata.containsKey("result")) {
                Object result = metadata.get("result");
                return result instanceof String ? (String) result : result.toString();
            } else {
                // Return entire metadata as JSON string if structure is different
                return metadata != null ? metadata.toString() : "User stories generated successfully";
            }
            
        } catch (HttpClientErrorException e) {
            log.error("MCP host HTTP error ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to call MCP agent: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to generate user stories via MCP: {}", e.getMessage(), e);
            throw new RuntimeException("Agent execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute any agent via MCP.
     * 
     * @param agentName The name of the agent to execute
     * @param arguments The arguments to pass to the agent
     * @return The response from the agent
     */
    public McpExecuteResponse executeAgent(String agentName, Map<String, Object> arguments) {
        log.info("Executing agent: {} with arguments: {}", agentName, arguments.keySet());
        
        try {
            MCPExecuteRequest request = new MCPExecuteRequest(agentName, arguments);
            
            String mcpHostUrl = mcpConfig.getMcpHostUrl();
            if (mcpHostUrl == null || mcpHostUrl.isEmpty()) {
                throw new RuntimeException("MCP host URL not configured");
            }
            
            String executeUrl = mcpHostUrl + "/execute";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<MCPExecuteRequest> httpEntity = new HttpEntity<>(request, headers);
            
            ResponseEntity<McpExecuteResponse> response = restTemplate.exchange(
                    executeUrl,
                    HttpMethod.POST,
                    httpEntity,
                    McpExecuteResponse.class
            );
            
            return response.getBody();
            
        } catch (Exception e) {
            log.error("Failed to execute agent {}: {}", agentName, e.getMessage(), e);
            throw new RuntimeException("Agent execution failed: " + e.getMessage(), e);
        }
    }
}
