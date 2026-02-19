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
import org.springframework.web.client.ResourceAccessException;

import java.util.HashMap;
import java.util.Map;

/**
 * Service to execute agents via MCP (Model Context Protocol) host.
 * Calls agents to generate artifacts like requirements, user stories, tech specs, etc.
 *
 * Agent flow:
 *   1. user-req-agent:     extracted text → structured requirements (FR, NFR, AC)
 *   2. user-stories-agent: requirements JSON → sprint-planned user stories
 *   3. tech-specs-agent:   requirements + stories → technical specification
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
     * Execute user-req-agent to generate structured requirements (FR, NFR, AC)
     * from parsed/extracted text.
     *
     * @param parsedText The parsed requirement text
     * @param jobId      The job ID for tracking
     * @return Map containing "requirements" (JSON), "requirements_text" (markdown), etc.
     */
    public Map<String, Object> generateRequirements(String parsedText, String jobId) {
        log.info("Calling user-req-agent for job: {}", jobId);

        String mcpHostUrl = mcpConfig.getMcpHostUrl();
        String executeUrl = null;

        try {
            Map<String, Object> arguments = new HashMap<>();
            arguments.put("requirement_text", parsedText);
            arguments.put("job_id", jobId);

            MCPExecuteRequest request = new MCPExecuteRequest("user-req-agent", arguments);

            if (mcpHostUrl == null || mcpHostUrl.isEmpty()) {
                throw new RuntimeException(
                        "MCP host URL not configured. Please set mcp.host.url in application.properties");
            }

            executeUrl = mcpHostUrl + "/execute";
            log.info("Calling MCP host at: {} for requirements generation", executeUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<MCPExecuteRequest> httpEntity = new HttpEntity<>(request, headers);

            ResponseEntity<McpExecuteResponse> response = restTemplate.exchange(
                    executeUrl,
                    HttpMethod.POST,
                    httpEntity,
                    McpExecuteResponse.class);

            McpExecuteResponse mcpResponse = response.getBody();
            if (mcpResponse == null) {
                throw new RuntimeException("Empty response from MCP host");
            }

            if (!"success".equalsIgnoreCase(mcpResponse.getStatus())) {
                String errorDetail = mcpResponse.getError() != null ? mcpResponse.getError() : mcpResponse.getStatus();
                throw new RuntimeException("Requirements agent execution failed: " + errorDetail);
            }

            Map<String, Object> metadata = mcpResponse.getMetadata();
            if (metadata == null) {
                throw new RuntimeException("No metadata returned from requirements agent");
            }

            log.info("Requirements generated successfully for job: {}", jobId);
            return metadata;

        } catch (ResourceAccessException e) {
            log.error("MCP host connection error: {}", e.getMessage(), e);
            String url = executeUrl != null ? executeUrl : (mcpHostUrl != null ? mcpHostUrl + "/execute" : "http://localhost:8080/execute");
            throw new RuntimeException("Cannot connect to MCP host at " + url + ". Please ensure the MCP host is running on port 8080. Error: " + e.getMessage(), e);
        } catch (HttpClientErrorException e) {
            log.error("MCP host HTTP error ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("MCP host returned error " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Failed to generate requirements via MCP: {}", e.getMessage(), e);
            throw new RuntimeException("Requirements agent execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute user-stories-agent to generate sprint-planned user stories
     * from structured requirements JSON.
     *
     * @param requirementsJson The structured requirements (as JSON string or object)
     * @param jobId            The job ID for tracking
     * @return Generated user stories as markdown string
     */
    public String generateUserStories(String requirementsJson, String jobId) {
        log.info("Calling user-stories-agent for job: {}", jobId);

        String mcpHostUrl = mcpConfig.getMcpHostUrl();
        String executeUrl = null;

        try {
            Map<String, Object> arguments = new HashMap<>();
            arguments.put("requirements_json", requirementsJson);
            arguments.put("job_id", jobId);

            MCPExecuteRequest request = new MCPExecuteRequest("user-stories-agent", arguments);

            if (mcpHostUrl == null || mcpHostUrl.isEmpty()) {
                throw new RuntimeException(
                        "MCP host URL not configured. Please set mcp.host.url in application.properties");
            }

            executeUrl = mcpHostUrl + "/execute";
            log.info("Calling MCP host at: {} for user stories", executeUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<MCPExecuteRequest> httpEntity = new HttpEntity<>(request, headers);

            ResponseEntity<McpExecuteResponse> response = restTemplate.exchange(
                    executeUrl,
                    HttpMethod.POST,
                    httpEntity,
                    McpExecuteResponse.class);

            McpExecuteResponse mcpResponse = response.getBody();
            if (mcpResponse == null) {
                throw new RuntimeException("Empty response from MCP host");
            }

            if (!"success".equalsIgnoreCase(mcpResponse.getStatus())) {
                String errorDetail = mcpResponse.getError() != null ? mcpResponse.getError() : mcpResponse.getStatus();
                throw new RuntimeException("User stories agent execution failed: " + errorDetail);
            }

            Map<String, Object> metadata = mcpResponse.getMetadata();
            if (metadata != null && metadata.containsKey("user_stories")) {
                Object stories = metadata.get("user_stories");
                return stories instanceof String ? (String) stories : stories.toString();
            } else if (metadata != null && metadata.containsKey("result")) {
                Object result = metadata.get("result");
                return result instanceof String ? (String) result : result.toString();
            } else {
                return metadata != null ? metadata.toString() : "User stories generated successfully";
            }

        } catch (ResourceAccessException e) {
            log.error("MCP host connection error: {}", e.getMessage(), e);
            String url = executeUrl != null ? executeUrl : (mcpHostUrl != null ? mcpHostUrl + "/execute" : "http://localhost:8080/execute");
            throw new RuntimeException("Cannot connect to MCP host at " + url + ". Please ensure the MCP host is running on port 8080. Error: " + e.getMessage(), e);
        } catch (HttpClientErrorException e) {
            log.error("MCP host HTTP error ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("MCP host returned error " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Failed to generate user stories via MCP: {}", e.getMessage(), e);
            throw new RuntimeException("User stories agent execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute the tech-specs-agent to generate a technical specification
     * from parsed requirements and user stories.
     *
     * @param parsedText  The parsed requirement text
     * @param userStories The generated user stories markdown
     * @param jobId       The job ID for tracking
     * @return Generated tech spec as markdown string
     */
    public String generateTechSpecs(String parsedText, String userStories, String jobId) {
        log.info("Calling tech specs agent for job: {}", jobId);

        String mcpHostUrl = mcpConfig.getMcpHostUrl();
        String executeUrl = null;

        try {
            Map<String, Object> arguments = new HashMap<>();
            arguments.put("requirement_text", parsedText);
            arguments.put("user_stories", userStories != null ? userStories : "");
            arguments.put("job_id", jobId);

            MCPExecuteRequest request = new MCPExecuteRequest("tech-specs-agent", arguments);

            if (mcpHostUrl == null || mcpHostUrl.isEmpty()) {
                throw new RuntimeException(
                        "MCP host URL not configured. Please set mcp.host.url in application.properties");
            }

            executeUrl = mcpHostUrl + "/execute";
            log.info("Calling MCP host at: {} for tech specs", executeUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<MCPExecuteRequest> httpEntity = new HttpEntity<>(request, headers);

            ResponseEntity<McpExecuteResponse> response = restTemplate.exchange(
                    executeUrl,
                    HttpMethod.POST,
                    httpEntity,
                    McpExecuteResponse.class);

            McpExecuteResponse mcpResponse = response.getBody();
            if (mcpResponse == null) {
                throw new RuntimeException("Empty response from MCP host");
            }

            if (!"success".equalsIgnoreCase(mcpResponse.getStatus())) {
                String errorDetail = mcpResponse.getError() != null ? mcpResponse.getError() : mcpResponse.getStatus();
                throw new RuntimeException("Tech specs agent execution failed: " + errorDetail);
            }

            Map<String, Object> metadata = mcpResponse.getMetadata();
            if (metadata != null && metadata.containsKey("tech_spec")) {
                Object spec = metadata.get("tech_spec");
                return spec instanceof String ? (String) spec : spec.toString();
            } else if (metadata != null && metadata.containsKey("result")) {
                Object result = metadata.get("result");
                return result instanceof String ? (String) result : result.toString();
            } else {
                return metadata != null ? metadata.toString() : "Tech spec generated successfully";
            }

        } catch (ResourceAccessException e) {
            log.error("MCP host connection error: {}", e.getMessage(), e);
            String url = executeUrl != null ? executeUrl : (mcpHostUrl != null ? mcpHostUrl + "/execute" : "http://localhost:8080/execute");
            throw new RuntimeException("Cannot connect to MCP host at " + url + ". Please ensure the MCP host is running on port 8080. Error: " + e.getMessage(), e);
        } catch (HttpClientErrorException e) {
            log.error("MCP host HTTP error ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("MCP host returned error " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Failed to generate tech specs via MCP: {}", e.getMessage(), e);
            throw new RuntimeException("Tech specs agent execution failed: " + e.getMessage(), e);
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
