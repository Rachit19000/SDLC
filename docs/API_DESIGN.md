# API Design Specification

## Base URL
```
Production: https://api.sdlc-automation.com/v1
Development: http://localhost:3000/api/v1
```

## Authentication
All endpoints (except auth endpoints) require JWT token in header:
```
Authorization: Bearer <jwt_token>
```

## WebSocket Connection
```
ws://localhost:3000/ws?token=<jwt_token>
```

---

## 1. Authentication APIs

### 1.1 Register User
```http
POST /auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securePassword123",
  "name": "John Doe"
}

Response: 201 Created
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "user_123",
    "email": "user@example.com",
    "name": "John Doe"
  }
}
```

### 1.2 Login
```http
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securePassword123"
}

Response: 200 OK
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "user_123",
    "email": "user@example.com",
    "name": "John Doe"
  }
}
```

---

## 2. Requirement Management APIs

### 2.1 Upload Requirement Document
```http
POST /requirements/upload
Content-Type: multipart/form-data

Form Data:
- file: <PDF/DOCX/TXT file>
- projectId: "proj_123" (optional, creates new if not provided)
- name: "E-commerce Platform Requirements"

Response: 202 Accepted
{
  "jobId": "job_abc123",
  "status": "processing",
  "message": "Document upload initiated. Use jobId to track progress via WebSocket."
}
```

### 2.2 Paste Requirement Text
```http
POST /requirements/paste
Content-Type: application/json

{
  "text": "We need to build an e-commerce platform...",
  "projectId": "proj_123",
  "name": "E-commerce Platform Requirements"
}

Response: 202 Accepted
{
  "jobId": "job_abc123",
  "status": "processing"
}
```

### 2.3 Get Parsed Requirement
```http
GET /requirements/{requirementId}/parsed

Response: 200 OK
{
  "id": "req_123",
  "projectId": "proj_123",
  "originalText": "Full extracted text...",
  "parsedAt": "2024-01-15T10:30:00Z",
  "status": "parsed"
}
```

### 2.4 Update Requirement Text
```http
PUT /requirements/{requirementId}/text
Content-Type: application/json

{
  "text": "Modified requirement text..."
}

Response: 200 OK
{
  "id": "req_123",
  "text": "Modified requirement text...",
  "updatedAt": "2024-01-15T11:00:00Z",
  "version": 2
}
```

---

## 3. Workflow Orchestration APIs

### 3.1 Start SDLC Workflow
```http
POST /workflows/start
Content-Type: application/json

{
  "requirementId": "req_123",
  "projectId": "proj_123",
  "config": {
    "skipValidation": false,
    "parallelAgents": false,
    "agentTimeout": 300000
  }
}

Response: 202 Accepted
{
  "workflowId": "wf_xyz789",
  "jobId": "job_abc123",
  "status": "started",
  "currentStep": "document_parsing",
  "estimatedTime": "15-20 minutes"
}
```

### 3.2 Get Workflow Status
```http
GET /workflows/{workflowId}

Response: 200 OK
{
  "id": "wf_xyz789",
  "status": "in_progress",
  "currentStep": "wbs_generation",
  "completedSteps": ["document_parsing", "text_extraction"],
  "pendingSteps": ["user_stories", "tech_spec", "nfr", "architecture", "sprint_plan", "test_scenarios", "performance_test", "workspace"],
  "progress": 25,
  "startedAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:35:00Z"
}
```

### 3.3 Get Workflow History
```http
GET /workflows/{workflowId}/history

Response: 200 OK
{
  "workflowId": "wf_xyz789",
  "steps": [
    {
      "stepId": "step_1",
      "agent": "document_parser",
      "status": "completed",
      "startedAt": "2024-01-15T10:30:00Z",
      "completedAt": "2024-01-15T10:31:00Z",
      "duration": 60,
      "validated": true,
      "validatedAt": "2024-01-15T10:32:00Z"
    },
    {
      "stepId": "step_2",
      "agent": "wbs_agent",
      "status": "in_progress",
      "startedAt": "2024-01-15T10:32:00Z"
    }
  ]
}
```

---

## 4. Artifact Management APIs

### 4.1 Get Artifact by Type
```http
GET /artifacts/{workflowId}/{artifactType}

Path Parameters:
- workflowId: Workflow identifier
- artifactType: wbs | user_stories | tech_spec | nfr | architecture | sprint_plan | test_scenarios | performance_test | workspace

Response: 200 OK
{
  "workflowId": "wf_xyz789",
  "artifactType": "wbs",
  "content": {
    // Structure varies by artifact type
    "tasks": [
      {
        "id": "task_1",
        "name": "User Authentication",
        "description": "Implement login/logout",
        "dependencies": [],
        "estimatedHours": 8
      }
    ]
  },
  "version": 1,
  "createdAt": "2024-01-15T10:35:00Z",
  "status": "pending_validation"
}
```

### 4.2 Update Artifact
```http
PUT /artifacts/{workflowId}/{artifactType}
Content-Type: application/json

{
  "content": {
    // Modified artifact content
  },
  "modificationNotes": "Added security requirements"
}

Response: 200 OK
{
  "workflowId": "wf_xyz789",
  "artifactType": "wbs",
  "version": 2,
  "updatedAt": "2024-01-15T11:00:00Z",
  "status": "modified",
  "githubCommit": {
    "sha": "abc123def456",
    "url": "https://github.com/org/repo/commit/abc123def456"
  }
}
```

### 4.3 Get All Artifacts
```http
GET /artifacts/{workflowId}

Response: 200 OK
{
  "workflowId": "wf_xyz789",
  "artifacts": [
    {
      "type": "wbs",
      "status": "validated",
      "version": 2,
      "createdAt": "2024-01-15T10:35:00Z"
    },
    {
      "type": "user_stories",
      "status": "pending_validation",
      "version": 1,
      "createdAt": "2024-01-15T10:40:00Z"
    }
  ]
}
```

---

## 5. Validation APIs

### 5.1 Validate Artifact
```http
POST /validation/validate
Content-Type: application/json

{
  "workflowId": "wf_xyz789",
  "stepId": "step_2",
  "artifactType": "wbs",
  "action": "approve", // approve | reject | modify
  "modifications": null, // Only if action is "modify"
  "notes": "Looks good, proceed"
}

Response: 200 OK
{
  "validationId": "val_123",
  "status": "approved",
  "nextStep": "user_stories_generation",
  "workflowStatus": "in_progress"
}
```

### 5.2 Reject Artifact
```http
POST /validation/reject
Content-Type: application/json

{
  "workflowId": "wf_xyz789",
  "stepId": "step_2",
  "artifactType": "wbs",
  "reason": "Missing security considerations",
  "retry": true
}

Response: 200 OK
{
  "validationId": "val_123",
  "status": "rejected",
  "retryTriggered": true,
  "newStepId": "step_2_retry_1"
}
```

### 5.3 Get Validation History
```http
GET /validation/{workflowId}/history

Response: 200 OK
{
  "workflowId": "wf_xyz789",
  "validations": [
    {
      "stepId": "step_1",
      "artifactType": "parsed_text",
      "action": "approve",
      "validatedBy": "user_123",
      "validatedAt": "2024-01-15T10:32:00Z"
    },
    {
      "stepId": "step_2",
      "artifactType": "wbs",
      "action": "modify",
      "validatedBy": "user_123",
      "validatedAt": "2024-01-15T10:37:00Z",
      "modifications": "Added security tasks"
    }
  ]
}
```

---

## 6. Agent Management APIs

### 6.1 Trigger Agent Manually
```http
POST /agents/trigger
Content-Type: application/json

{
  "workflowId": "wf_xyz789",
  "agentType": "wbs_agent",
  "input": {
    "requirementText": "...",
    "previousArtifacts": {}
  }
}

Response: 202 Accepted
{
  "agentJobId": "agent_job_456",
  "status": "queued",
  "estimatedTime": "2-3 minutes"
}
```

### 6.2 Get Agent Status
```http
GET /agents/{agentJobId}

Response: 200 OK
{
  "agentJobId": "agent_job_456",
  "status": "processing",
  "progress": 60,
  "startedAt": "2024-01-15T10:35:00Z",
  "estimatedCompletion": "2024-01-15T10:37:00Z"
}
```

### 6.3 Cancel Agent Job
```http
POST /agents/{agentJobId}/cancel

Response: 200 OK
{
  "agentJobId": "agent_job_456",
  "status": "cancelled",
  "cancelledAt": "2024-01-15T10:36:00Z"
}
```

---

## 7. Version Management APIs (GitHub Integration)

### 7.1 Initialize GitHub Repository
```http
POST /version/initialize
Content-Type: application/json

{
  "workflowId": "wf_xyz789",
  "repositoryName": "ecommerce-platform",
  "organization": "my-org",
  "private": true
}

Response: 201 Created
{
  "repositoryId": "repo_123",
  "repositoryUrl": "https://github.com/my-org/ecommerce-platform",
  "branch": "main",
  "initialCommit": "abc123def456"
}
```

### 7.2 Get Version History
```http
GET /version/{workflowId}/history

Response: 200 OK
{
  "workflowId": "wf_xyz789",
  "commits": [
    {
      "sha": "abc123def456",
      "message": "Initial requirement document",
      "author": "SDLC Bot",
      "timestamp": "2024-01-15T10:30:00Z",
      "artifacts": ["parsed_text"]
    },
    {
      "sha": "def456ghi789",
      "message": "WBS generated and validated",
      "author": "SDLC Bot",
      "timestamp": "2024-01-15T10:38:00Z",
      "artifacts": ["wbs"]
    }
  ]
}
```

### 7.3 Get Latest Version
```http
GET /version/{workflowId}/latest

Response: 200 OK
{
  "workflowId": "wf_xyz789",
  "latestCommit": "def456ghi789",
  "branch": "main",
  "repositoryUrl": "https://github.com/my-org/ecommerce-platform",
  "artifacts": ["parsed_text", "wbs"],
  "lastUpdated": "2024-01-15T10:38:00Z"
}
```

---

## 8. Workspace Management APIs

### 8.1 Generate Workspace Structure
```http
POST /workspace/generate
Content-Type: application/json

{
  "workflowId": "wf_xyz789",
  "techStack": {
    "frontend": "react",
    "backend": "nodejs",
    "database": "postgresql"
  },
  "structureType": "monorepo" // monorepo | microservices
}

Response: 202 Accepted
{
  "workspaceJobId": "ws_job_789",
  "status": "processing"
}
```

### 8.2 Get Workspace Structure
```http
GET /workspace/{workflowId}

Response: 200 OK
{
  "workflowId": "wf_xyz789",
  "structure": {
    "type": "monorepo",
    "directories": [
      {
        "path": "frontend",
        "type": "directory",
        "children": [
          {
            "path": "frontend/src",
            "type": "directory"
          },
          {
            "path": "frontend/package.json",
            "type": "file"
          }
        ]
      },
      {
        "path": "backend",
        "type": "directory"
      }
    ],
    "githubPath": "https://github.com/my-org/ecommerce-platform/tree/main"
  }
}
```

---

## 9. Project Management APIs

### 9.1 Create Project
```http
POST /projects
Content-Type: application/json

{
  "name": "E-commerce Platform",
  "description": "Modern e-commerce solution",
  "teamMembers": ["user_123", "user_456"]
}

Response: 201 Created
{
  "id": "proj_123",
  "name": "E-commerce Platform",
  "createdAt": "2024-01-15T10:00:00Z",
  "owner": "user_123"
}
```

### 9.2 Get Project Details
```http
GET /projects/{projectId}

Response: 200 OK
{
  "id": "proj_123",
  "name": "E-commerce Platform",
  "description": "Modern e-commerce solution",
  "workflows": [
    {
      "id": "wf_xyz789",
      "status": "in_progress",
      "createdAt": "2024-01-15T10:30:00Z"
    }
  ],
  "teamMembers": [
    {
      "id": "user_123",
      "name": "John Doe",
      "role": "owner"
    }
  ]
}
```

### 9.3 List Projects
```http
GET /projects?page=1&limit=20&status=active

Response: 200 OK
{
  "projects": [
    {
      "id": "proj_123",
      "name": "E-commerce Platform",
      "status": "active",
      "lastActivity": "2024-01-15T10:35:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 5,
    "totalPages": 1
  }
}
```

---

## 10. WebSocket Events

### Connection
```javascript
const socket = io('ws://localhost:3000/ws', {
  auth: { token: 'jwt_token' }
});
```

### Event: workflow.progress
```javascript
socket.on('workflow.progress', (data) => {
  {
    "workflowId": "wf_xyz789",
    "step": "wbs_generation",
    "progress": 60,
    "status": "processing",
    "message": "Generating work breakdown structure..."
  }
});
```

### Event: workflow.step.complete
```javascript
socket.on('workflow.step.complete', (data) => {
  {
    "workflowId": "wf_xyz789",
    "stepId": "step_2",
    "agent": "wbs_agent",
    "artifactType": "wbs",
    "status": "ready_for_validation",
    "artifactUrl": "/api/v1/artifacts/wf_xyz789/wbs"
  }
});
```

### Event: workflow.validation.required
```javascript
socket.on('workflow.validation.required', (data) => {
  {
    "workflowId": "wf_xyz789",
    "stepId": "step_2",
    "artifactType": "wbs",
    "message": "Please review and validate the generated WBS"
  }
});
```

### Event: workflow.validation.complete
```javascript
socket.on('workflow.validation.complete', (data) => {
  {
    "workflowId": "wf_xyz789",
    "stepId": "step_2",
    "action": "approved",
    "nextStep": "user_stories_generation"
  }
});
```

### Event: workflow.complete
```javascript
socket.on('workflow.complete', (data) => {
  {
    "workflowId": "wf_xyz789",
    "status": "completed",
    "completedAt": "2024-01-15T11:00:00Z",
    "artifacts": ["wbs", "user_stories", "tech_spec", "nfr", "architecture", "sprint_plan", "test_scenarios", "performance_test", "workspace"]
  }
});
```

### Event: workflow.error
```javascript
socket.on('workflow.error', (data) => {
  {
    "workflowId": "wf_xyz789",
    "stepId": "step_2",
    "error": "Agent timeout",
    "retryable": true,
    "message": "WBS agent timed out. Retrying..."
  }
});
```

---

## 11. Error Responses

### Standard Error Format
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input provided",
    "details": {
      "field": "requirementId",
      "reason": "Requirement not found"
    },
    "timestamp": "2024-01-15T10:30:00Z"
  }
}
```

### Common Error Codes
- `400 BAD_REQUEST`: Invalid input
- `401 UNAUTHORIZED`: Missing or invalid token
- `403 FORBIDDEN`: Insufficient permissions
- `404 NOT_FOUND`: Resource not found
- `409 CONFLICT`: Resource conflict
- `422 UNPROCESSABLE_ENTITY`: Validation error
- `429 TOO_MANY_REQUESTS`: Rate limit exceeded
- `500 INTERNAL_SERVER_ERROR`: Server error
- `503 SERVICE_UNAVAILABLE`: Service temporarily unavailable
- `504 GATEWAY_TIMEOUT`: Agent timeout

---

## 12. Rate Limiting

- **Standard Users**: 100 requests/minute
- **Premium Users**: 500 requests/minute
- **WebSocket Connections**: 10 concurrent connections per user

Rate limit headers:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1642248000
```
