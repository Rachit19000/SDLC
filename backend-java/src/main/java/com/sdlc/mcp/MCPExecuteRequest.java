package com.sdlc.mcp;

import java.util.Map;

public class MCPExecuteRequest {

    private String agent;
    private Map<String, Object> arguments;

    public MCPExecuteRequest() {}

    public MCPExecuteRequest(String agent, Map<String, Object> arguments) {
        this.agent = agent;
        this.arguments = arguments;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }
}
