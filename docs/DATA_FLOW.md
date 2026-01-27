# Data Flow & State Management Design

## 1. End-to-End Data Flow

### 1.1 Requirement Upload to Artifact Generation

```
┌──────────────┐
│   Frontend   │
│  (User UI)   │
└──────┬───────┘
       │ 1. Upload File/Paste Text
       │ POST /requirements/upload
       ↓
┌──────────────────────┐
│   API Gateway        │
│  (Auth + Routing)    │
└──────┬───────────────┘
       │ 2. Forward Request
       ↓
┌──────────────────────┐
│  Orchestrator        │
│  Service             │
└──────┬───────────────┘
       │ 3. Create Workflow
       │ 4. Store in PostgreSQL
       │ 5. Queue Job (Redis)
       ↓
┌──────────────────────┐
│  Document Parser     │
│  Agent               │
└──────┬───────────────┘
       │ 6. Extract Text
       │ 7. Store in S3
       │ 8. Save Metadata (PostgreSQL)
       ↓
┌──────────────────────┐
│  Artifact Manager    │
│  Service             │
└──────┬───────────────┘
       │ 9. Store Artifact
       │ 10. Update Workflow State
       │ 11. Publish WebSocket Event
       ↓
┌──────────────────────┐
│   Frontend           │
│  (WebSocket Listener)│
└──────────────────────┘
       │ 12. Display for Validation
       ↓
┌──────────────────────┐
│   User Validates     │
│   (Approve/Modify)   │
└──────┬───────────────┘
       │ 13. POST /validation/validate
       ↓
┌──────────────────────┐
│  Validation Service  │
└──────┬───────────────┘
       │ 14. If Modified:
       │     - Save to GitHub
       │     - Update Version
       │ 15. Trigger Next Agent
       ↓
┌──────────────────────┐
│  Next Agent (WBS)    │
│  - Fetch Latest      │
│    Version from      │
│    GitHub            │
│  - Process           │
│  - Generate Artifact │
└──────────────────────┘
```

## 2. State Management Architecture

### 2.1 State Storage Layers

```
┌─────────────────────────────────────────────────────────┐
│              STATE STORAGE LAYERS                        │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │  PostgreSQL (Persistent State)                   │  │
│  │  - Workflow metadata                             │  │
│  │  - Artifact metadata                             │  │
│  │  - Validation history                            │  │
│  │  - User/project data                             │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Redis (Ephemeral State)                         │  │
│  │  - Job queues                                    │  │
│  │  - Active workflow state                         │  │
│  │  - WebSocket session state                       │  │
│  │  - Cache (agent responses)                       │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │  S3/MinIO (File Storage)                         │  │
│  │  - Original requirement documents                │  │
│  │  - Generated artifact files                      │  │
│  │  - Large content blobs                           │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │  GitHub (Version Control)                        │  │
│  │  - Committed artifacts                           │  │
│  │  - Version history                               │  │
│  │  - Workspace structure                           │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### 2.2 State Transition Model

```typescript
enum WorkflowStatus {
  INITIAL = "initial",
  PARSING = "parsing",
  PARSED = "parsed",
  WBS_GENERATING = "wbs_generating",
  WBS_READY = "wbs_ready",
  WBS_VALIDATING = "wbs_validating",
  USER_STORIES_GENERATING = "user_stories_generating",
  USER_STORIES_READY = "user_stories_ready",
  // ... more states
  COMPLETED = "completed",
  FAILED = "failed",
  PAUSED = "paused"
}

enum ArtifactStatus {
  GENERATING = "generating",
  READY = "ready",
  PENDING_VALIDATION = "pending_validation",
  VALIDATED = "validated",
  MODIFIED = "modified",
  REJECTED = "rejected"
}

enum ValidationAction {
  APPROVE = "approve",
  REJECT = "reject",
  MODIFY = "modify"
}
```

## 3. Database Schema Design

### 3.1 Core Tables

```sql
-- Projects
CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id UUID NOT NULL REFERENCES users(id),
    github_repo_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Requirements
CREATE TABLE requirements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id),
    name VARCHAR(255) NOT NULL,
    original_file_url TEXT, -- S3 URL
    original_text TEXT,
    parsed_text TEXT,
    file_type VARCHAR(10), -- pdf, docx, txt
    status VARCHAR(20) DEFAULT 'uploaded',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Workflows
CREATE TABLE workflows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id),
    requirement_id UUID NOT NULL REFERENCES requirements(id),
    status VARCHAR(50) NOT NULL DEFAULT 'initial',
    current_step VARCHAR(50),
    progress INTEGER DEFAULT 0,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Workflow Steps
CREATE TABLE workflow_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES workflows(id),
    step_name VARCHAR(50) NOT NULL,
    step_order INTEGER NOT NULL,
    agent_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    duration_seconds INTEGER,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(workflow_id, step_order)
);

-- Artifacts
CREATE TABLE artifacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES workflows(id),
    workflow_step_id UUID REFERENCES workflow_steps(id),
    artifact_type VARCHAR(50) NOT NULL,
    content JSONB NOT NULL,
    file_url TEXT, -- S3 URL if large
    version INTEGER DEFAULT 1,
    status VARCHAR(20) DEFAULT 'generating',
    github_commit_sha VARCHAR(40),
    github_commit_url TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(workflow_id, artifact_type, version)
);

-- Validations
CREATE TABLE validations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES workflows(id),
    workflow_step_id UUID NOT NULL REFERENCES workflow_steps(id),
    artifact_id UUID NOT NULL REFERENCES artifacts(id),
    validated_by UUID NOT NULL REFERENCES users(id),
    action VARCHAR(20) NOT NULL, -- approve, reject, modify
    notes TEXT,
    modifications JSONB, -- If modified, store changes
    validated_at TIMESTAMP DEFAULT NOW()
);

-- Version History (GitHub Integration)
CREATE TABLE version_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES workflows(id),
    artifact_id UUID REFERENCES artifacts(id),
    github_commit_sha VARCHAR(40) NOT NULL,
    github_commit_url TEXT,
    commit_message TEXT,
    committed_at TIMESTAMP NOT NULL,
    committed_by VARCHAR(100) DEFAULT 'SDLC Bot'
);

-- Agent Jobs (Redis Queue Metadata)
CREATE TABLE agent_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES workflows(id),
    workflow_step_id UUID REFERENCES workflow_steps(id),
    agent_type VARCHAR(50) NOT NULL,
    redis_job_id VARCHAR(100),
    status VARCHAR(20) DEFAULT 'queued',
    input_data JSONB,
    output_data JSONB,
    error_data JSONB,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);
```

### 3.2 Indexes for Performance

```sql
CREATE INDEX idx_workflows_project ON workflows(project_id);
CREATE INDEX idx_workflows_status ON workflows(status);
CREATE INDEX idx_workflow_steps_workflow ON workflow_steps(workflow_id);
CREATE INDEX idx_artifacts_workflow_type ON artifacts(workflow_id, artifact_type);
CREATE INDEX idx_artifacts_status ON artifacts(status);
CREATE INDEX idx_validations_workflow ON validations(workflow_id);
CREATE INDEX idx_version_history_workflow ON version_history(workflow_id);
```

## 4. Redis State Management

### 4.1 Queue Structure

```python
# Job Queue Keys
WORKFLOW_QUEUE = "workflow:queue"
AGENT_QUEUE_PREFIX = "agent:queue:{agent_type}"
RETRY_QUEUE = "workflow:retry"

# State Keys
WORKFLOW_STATE_KEY = "workflow:state:{workflow_id}"
AGENT_JOB_KEY = "agent:job:{job_id}"
WEBSOCKET_SESSION_KEY = "ws:session:{user_id}:{workflow_id}"

# Cache Keys
ARTIFACT_CACHE_KEY = "cache:artifact:{workflow_id}:{artifact_type}"
REQUIREMENT_CACHE_KEY = "cache:requirement:{requirement_id}"
```

### 4.2 Redis Data Structures

```python
# Workflow State (Hash)
{
    "workflow:state:wf_123": {
        "status": "wbs_generating",
        "current_step": "wbs_generation",
        "progress": 25,
        "last_updated": "2024-01-15T10:35:00Z"
    }
}

# Job Queue (List)
# Using Bull/BullMQ structure
{
    "workflow:queue": [
        {
            "id": "job_456",
            "data": {
                "workflow_id": "wf_123",
                "agent_type": "wbs_agent",
                "input": {...}
            },
            "opts": {
                "attempts": 3,
                "timeout": 300000
            }
        }
    ]
}

# WebSocket Sessions (Set)
{
    "ws:sessions:wf_123": ["user_1", "user_2"]
}
```

## 5. Data Flow Between Components

### 5.1 Agent Input Preparation

```python
async def prepare_agent_input(workflow_id: str, agent_type: str) -> AgentInput:
    # 1. Get workflow from PostgreSQL
    workflow = await db.get_workflow(workflow_id)
    
    # 2. Get requirement text
    requirement = await db.get_requirement(workflow.requirement_id)
    
    # 3. Get latest version from GitHub
    latest_artifacts = await version_manager.get_latest_artifacts(workflow_id)
    
    # 4. Get previous artifacts from DB (for context)
    previous_artifacts = await db.get_artifacts_by_workflow(workflow_id)
    
    # 5. Combine into agent input
    return AgentInput(
        workflowId=workflow_id,
        requirementId=workflow.requirement_id,
        requirementText=requirement.parsed_text,
        previousArtifacts={
            **latest_artifacts,  # From GitHub (latest validated)
            **previous_artifacts  # From DB (for metadata)
        },
        context={
            "projectName": workflow.project.name,
            "techStack": workflow.project.tech_stack
        }
    )
```

### 5.2 Artifact Storage Flow

```python
async def store_artifact(workflow_id: str, agent_output: AgentOutput):
    # 1. Determine storage strategy
    artifact = agent_output.artifact
    
    # 2. Store large content in S3
    if len(json.dumps(artifact.content)) > 100000:  # > 100KB
        s3_url = await s3.upload(
            key=f"artifacts/{workflow_id}/{artifact.type}.json",
            content=json.dumps(artifact.content)
        )
        file_url = s3_url
        content = None  # Don't store in DB
    else:
        file_url = None
        content = artifact.content
    
    # 3. Store metadata in PostgreSQL
    artifact_record = await db.create_artifact(
        workflow_id=workflow_id,
        artifact_type=artifact.type,
        content=content,
        file_url=file_url,
        status="ready"
    )
    
    # 4. Update workflow state in Redis (fast access)
    await redis.hset(
        f"workflow:state:{workflow_id}",
        mapping={
            "current_step": artifact.type,
            "status": "ready_for_validation",
            "last_updated": datetime.utcnow().isoformat()
        }
    )
    
    # 5. Publish WebSocket event
    await websocket.publish(
        f"workflow.{workflow_id}.artifact.ready",
        {
            "artifact_type": artifact.type,
            "artifact_id": artifact_record.id,
            "status": "pending_validation"
        }
    )
    
    return artifact_record
```

## 6. Version Management Data Flow

### 6.1 Version Save Flow

```
User Modifies Artifact
        │
        ↓
Validation Service
        │
        ↓
┌───────────────────────┐
│  Check if Modified    │
└───────┬───────────────┘
        │ Yes
        ↓
┌───────────────────────┐
│  Prepare Commit       │
│  - Artifact Content   │
│  - Commit Message     │
│  - File Path          │
└───────┬───────────────┘
        │
        ↓
┌───────────────────────┐
│  GitHub API Call      │
│  Create Commit        │
└───────┬───────────────┘
        │
        ↓
┌───────────────────────┐
│  Store Commit Info    │
│  in PostgreSQL        │
│  (version_history)    │
└───────┬───────────────┘
        │
        ↓
┌───────────────────────┐
│  Update Artifact      │
│  - github_commit_sha  │
│  - version++          │
│  - status = validated │
└───────┬───────────────┘
        │
        ↓
┌───────────────────────┐
│  Update Latest        │
│  Version Reference    │
│  (for next agent)     │
└───────────────────────┘
```

### 6.2 Version Retrieval Flow

```python
async def get_latest_artifacts_for_agent(workflow_id: str) -> dict:
    # 1. Get latest commit SHA from version_history
    latest_version = await db.query("""
        SELECT github_commit_sha, committed_at
        FROM version_history
        WHERE workflow_id = $1
        ORDER BY committed_at DESC
        LIMIT 1
    """, workflow_id)
    
    if not latest_version:
        # No versions yet, return empty
        return {}
    
    # 2. Fetch tree from GitHub at that commit
    tree = await github_client.get_tree(
        repo=workflow_id,
        sha=latest_version['github_commit_sha']
    )
    
    # 3. Extract artifact files
    artifacts = {}
    for item in tree.tree:
        if item.path.startswith("artifacts/") and item.path.endswith(".json"):
            artifact_type = item.path.replace("artifacts/", "").replace(".json", "")
            
            # 4. Get file content from GitHub
            content = await github_client.get_blob(
                repo=workflow_id,
                sha=item.sha
            )
            
            artifacts[artifact_type] = json.loads(content)
    
    return artifacts
```

## 7. WebSocket Event Flow

### 7.1 Event Publishing

```python
class WebSocketManager:
    async def publish_workflow_event(self, workflow_id: str, event_type: str, data: dict):
        # 1. Get all connected clients for this workflow
        clients = await redis.smembers(f"ws:sessions:{workflow_id}")
        
        # 2. Publish to each client
        for client_id in clients:
            await redis.publish(
                f"ws:client:{client_id}",
                json.dumps({
                    "type": event_type,
                    "workflow_id": workflow_id,
                    "data": data,
                    "timestamp": datetime.utcnow().isoformat()
                })
            )
    
    async def publish_validation_required(self, workflow_id: str, artifact_type: str):
        await self.publish_workflow_event(
            workflow_id,
            "workflow.validation.required",
            {
                "artifact_type": artifact_type,
                "message": f"Please review and validate the generated {artifact_type}"
            }
        )
```

### 7.2 Frontend Event Handling

```typescript
// Frontend WebSocket Client
const socket = io('ws://localhost:3000/ws', {
  auth: { token: userToken }
});

// Subscribe to workflow events
socket.on('workflow.progress', (data) => {
  updateProgressBar(data.progress);
  updateStatusMessage(data.message);
});

socket.on('workflow.validation.required', (data) => {
  showValidationModal(data.artifact_type);
  loadArtifactForValidation(data.artifact_type);
});

socket.on('workflow.step.complete', (data) => {
  updateArtifactList(data.artifact_type);
  showNotification(`${data.artifact_type} generated successfully`);
});

socket.on('workflow.complete', (data) => {
  showCompletionScreen(data.artifacts);
});
```

## 8. Cache Strategy

### 8.1 Caching Layers

```python
# L1: In-Memory Cache (FastAPI/Node.js)
@lru_cache(maxsize=100)
def get_workflow_metadata(workflow_id: str):
    return db.get_workflow(workflow_id)

# L2: Redis Cache (Distributed)
async def get_cached_artifact(workflow_id: str, artifact_type: str):
    cache_key = f"cache:artifact:{workflow_id}:{artifact_type}"
    
    # Try Redis first
    cached = await redis.get(cache_key)
    if cached:
        return json.loads(cached)
    
    # Cache miss - fetch from DB
    artifact = await db.get_artifact(workflow_id, artifact_type)
    
    # Store in cache (TTL: 1 hour)
    await redis.setex(
        cache_key,
        3600,
        json.dumps(artifact)
    )
    
    return artifact
```

### 8.2 Cache Invalidation

```python
async def invalidate_artifact_cache(workflow_id: str, artifact_type: str):
    cache_key = f"cache:artifact:{workflow_type}:{artifact_type}"
    await redis.delete(cache_key)
    
    # Also invalidate workflow state cache
    await redis.delete(f"workflow:state:{workflow_id}")
```

## 9. Data Consistency

### 9.1 Transaction Management

```python
async def validate_and_save(workflow_id: str, artifact_id: str, action: str):
    async with db.transaction():
        # 1. Update validation record
        validation = await db.create_validation(
            workflow_id=workflow_id,
            artifact_id=artifact_id,
            action=action
        )
        
        # 2. Update artifact status
        await db.update_artifact(
            artifact_id,
            status="validated" if action == "approve" else "rejected"
        )
        
        # 3. Update workflow step
        await db.update_workflow_step(
            workflow_id=workflow_id,
            step_name=artifact.type,
            validation_status=action
        )
        
        # 4. If modified, save to GitHub (outside transaction)
        if action == "modify":
            await version_manager.save_version(workflow_id, artifact_id)
```

### 9.2 Eventual Consistency

- **PostgreSQL**: Source of truth for persistent data
- **Redis**: Eventually consistent (can be rebuilt from PostgreSQL)
- **GitHub**: Eventually consistent (commits are async)
- **S3**: Eventually consistent (files are immutable)

## 10. Data Backup & Recovery

### 10.1 Backup Strategy

1. **PostgreSQL**: Daily automated backups
2. **S3**: Versioned buckets with lifecycle policies
3. **GitHub**: Built-in version control
4. **Redis**: Not backed up (ephemeral, can be rebuilt)

### 10.2 Recovery Procedures

```python
async def recover_workflow_state(workflow_id: str):
    # 1. Load from PostgreSQL (source of truth)
    workflow = await db.get_workflow(workflow_id)
    
    # 2. Rebuild Redis state
    await redis.hset(
        f"workflow:state:{workflow_id}",
        mapping={
            "status": workflow.status,
            "current_step": workflow.current_step,
            "progress": workflow.progress
        }
    )
    
    # 3. Rebuild job queues if needed
    if workflow.status == "in_progress":
        await queue.rebuild_jobs(workflow_id)
```
