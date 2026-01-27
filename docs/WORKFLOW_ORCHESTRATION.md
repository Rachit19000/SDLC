# Agent Orchestration & Workflow Design

## 1. Workflow State Machine

```
┌─────────────┐
│   INITIAL   │
└──────┬──────┘
       │ Upload/Paste Requirement
       ↓
┌─────────────────┐
│ DOCUMENT_PARSING │
└──────┬──────────┘
       │ Parse Complete
       ↓
┌──────────────────┐
│ PARSED_TEXT_READY │
└──────┬───────────┘
       │ [Human Validation]
       ↓
┌──────────────────┐
│  WBS_GENERATION  │
└──────┬───────────┘
       │ WBS Generated
       ↓
┌──────────────────┐
│   WBS_READY      │
└──────┬───────────┘
       │ [Human Validation]
       ↓
┌─────────────────────┐
│ USER_STORIES_GEN     │
└──────┬──────────────┘
       │ Stories Generated
       ↓
┌─────────────────────┐
│ USER_STORIES_READY   │
└──────┬──────────────┘
       │ [Human Validation]
       ↓
┌─────────────────────┐
│  TECH_SPEC_GEN      │
└──────┬──────────────┘
       │ Spec Generated
       ↓
┌─────────────────────┐
│  TECH_SPEC_READY    │
└──────┬──────────────┘
       │ [Human Validation]
       ↓
┌─────────────────────┐
│   NFR_GENERATION    │
└──────┬──────────────┘
       │ NFR Generated
       ↓
┌─────────────────────┐
│    NFR_READY        │
└──────┬──────────────┘
       │ [Human Validation]
       ↓
┌─────────────────────┐
│ ARCHITECTURE_GEN    │
└──────┬──────────────┘
       │ Architecture Generated
       ↓
┌─────────────────────┐
│ ARCHITECTURE_READY  │
└──────┬──────────────┘
       │ [Human Validation]
       ↓
┌─────────────────────┐
│  SPRINT_PLAN_GEN    │
└──────┬──────────────┘
       │ Plan Generated
       ↓
┌─────────────────────┐
│ SPRINT_PLAN_READY   │
└──────┬──────────────┘
       │ [Human Validation]
       ↓
┌─────────────────────┐
│ TEST_SCENARIOS_GEN  │
└──────┬──────────────┘
       │ Scenarios Generated
       ↓
┌─────────────────────┐
│ TEST_SCENARIOS_READY │
└──────┬───────────────┘
       │ [Human Validation]
       ↓
┌─────────────────────┐
│ PERFORMANCE_TEST_GEN│
└──────┬──────────────┘
       │ Approach Generated
       ↓
┌─────────────────────┐
│ PERFORMANCE_TEST_READY│
└──────┬──────────────┘
       │ [Human Validation]
       ↓
┌─────────────────────┐
│ WORKSPACE_GEN       │
└──────┬──────────────┘
       │ Workspace Generated
       ↓
┌─────────────────────┐
│   WORKSPACE_READY    │
└──────┬───────────────┘
       │ [Human Validation]
       ↓
┌─────────────────────┐
│     COMPLETED       │
└─────────────────────┘
```

## 2. Agent Orchestration Pattern

### 2.1 Sequential Agent Execution

```python
class WorkflowOrchestrator:
    def __init__(self):
        self.agents = [
            DocumentParserAgent(),
            WBSAgent(),
            UserStoriesAgent(),
            TechSpecAgent(),
            NFRAgent(),
            ArchitectureAgent(),
            SprintPlanAgent(),
            TestScenariosAgent(),
            PerformanceTestAgent(),
            WorkspaceAgent()
        ]
    
    async def execute_workflow(self, workflow_id, requirement_id):
        workflow_state = WorkflowState(workflow_id, requirement_id)
        
        for agent in self.agents:
            # Wait for validation if required
            if workflow_state.requires_validation:
                await self.wait_for_validation(workflow_state)
            
            # Execute agent
            result = await self.execute_agent(agent, workflow_state)
            
            # Store artifact
            artifact = await self.store_artifact(workflow_state, result)
            
            # Trigger validation checkpoint
            await self.trigger_validation(workflow_state, artifact)
            
            # Update workflow state
            workflow_state.move_to_next_step()
            
            # Save version if modified
            if artifact.is_modified:
                await self.save_version(workflow_state, artifact)
```

### 2.2 Agent Execution Flow

```
┌─────────────────────────────────────────────────────────┐
│              Orchestrator Service                        │
└─────────────────────────────────────────────────────────┘
                    │
                    ↓
        ┌───────────────────────┐
        │  Create Agent Job    │
        │  (Redis Queue)       │
        └───────────┬──────────┘
                    │
                    ↓
        ┌───────────────────────┐
        │  Agent Pool Manager   │
        │  - Select Agent       │
        │  - Check Health       │
        │  - Assign Job         │
        └───────────┬───────────┘
                    │
                    ↓
        ┌───────────────────────┐
        │   Agent Service       │
        │  - Process Request    │
        │  - Call AI API        │
        │  - Handle Timeout     │
        └───────────┬───────────┘
                    │
                    ↓
        ┌───────────────────────┐
        │   Result Handler      │
        │  - Store Artifact     │
        │  - Update State       │
        │  - Publish Event      │
        └───────────┬───────────┘
                    │
                    ↓
        ┌───────────────────────┐
        │  Validation Trigger   │
        │  (WebSocket Event)    │
        └───────────────────────┘
```

## 3. Agent Input/Output Schema

### 3.1 Agent Input Structure

```typescript
interface AgentInput {
  workflowId: string;
  requirementId: string;
  requirementText: string;
  previousArtifacts: {
    [key: string]: Artifact;
  };
  context: {
    projectName: string;
    techStack?: string[];
    teamSize?: number;
  };
  config: {
    timeout: number;
    retryCount: number;
    model: string; // "gpt-4", "claude-3", etc.
  };
}
```

### 3.2 Agent Output Structure

```typescript
interface AgentOutput {
  agentType: string;
  workflowId: string;
  artifact: {
    type: ArtifactType;
    content: any; // Type-specific content
    metadata: {
      generatedAt: string;
      model: string;
      tokensUsed: number;
      processingTime: number;
    };
  };
  status: "success" | "error" | "timeout";
  error?: {
    code: string;
    message: string;
    retryable: boolean;
  };
}
```

## 4. Agent-Specific Implementations

### 4.1 Document Parser Agent

```python
class DocumentParserAgent:
    async def process(self, input: AgentInput) -> AgentOutput:
        # Extract file from storage
        file = await storage.get(input.requirementId)
        
        # Parse based on file type
        if file.type == "pdf":
            text = await pdf_parser.extract(file)
        elif file.type == "docx":
            text = await docx_parser.extract(file)
        else:
            text = file.content
        
        # Clean and structure text
        cleaned_text = self.clean_text(text)
        
        return AgentOutput(
            artifact=Artifact(
                type="parsed_text",
                content={"text": cleaned_text}
            )
        )
```

### 4.2 WBS Agent

```python
class WBSAgent:
    async def process(self, input: AgentInput) -> AgentOutput:
        prompt = f"""
        Based on the following requirements, generate a Work Breakdown Structure:
        
        Requirements:
        {input.requirementText}
        
        Previous context:
        {input.previousArtifacts}
        
        Generate a hierarchical WBS with:
        - Tasks
        - Subtasks
        - Dependencies
        - Estimated hours
        - Priority
        """
        
        response = await ai_client.generate(
            model="gpt-4",
            prompt=prompt,
            temperature=0.3
        )
        
        wbs = self.parse_wbs_response(response)
        
        return AgentOutput(
            artifact=Artifact(
                type="wbs",
                content=wbs
            )
        )
```

### 4.3 User Stories Agent

```python
class UserStoriesAgent:
    async def process(self, input: AgentInput) -> AgentOutput:
        prompt = f"""
        Generate user stories (Epics and Stories) from:
        
        Requirements: {input.requirementText}
        WBS: {input.previousArtifacts.get('wbs')}
        
        Format:
        - Epic: [Epic Name]
          - Story 1: As a [user], I want [feature] so that [benefit]
          - Story 2: ...
        """
        
        response = await ai_client.generate(
            model="gpt-4",
            prompt=prompt
        )
        
        stories = self.parse_stories(response)
        
        return AgentOutput(
            artifact=Artifact(
                type="user_stories",
                content=stories
            )
        )
```

## 5. Validation Checkpoint Flow

```
┌─────────────────────────────────────────────────────────┐
│              Agent Completes Processing                  │
└───────────────────────┬─────────────────────────────────┘
                        │
                        ↓
        ┌───────────────────────────────┐
        │   Store Artifact in DB        │
        │   Status: pending_validation  │
        └───────────────┬───────────────┘
                        │
                        ↓
        ┌───────────────────────────────┐
        │   Publish WebSocket Event     │
        │   workflow.validation.required│
        └───────────────┬───────────────┘
                        │
                        ↓
        ┌───────────────────────────────┐
        │   Frontend Receives Event     │
        │   Shows Validation UI         │
        └───────────────┬───────────────┘
                        │
                        ↓
        ┌───────────────────────────────┐
        │   Human Reviews Artifact      │
        │   - Approve                   │
        │   - Reject                    │
        │   - Modify                    │
        └───────────────┬───────────────┘
                        │
                        ↓
        ┌───────────────────────────────┐
        │   POST /validation/validate   │
        └───────────────┬───────────────┘
                        │
            ┌───────────┼───────────┐
            │           │           │
        [Approve]   [Reject]   [Modify]
            │           │           │
            ↓           ↓           ↓
    ┌───────────┐ ┌──────────┐ ┌──────────┐
    │ Continue  │ │ Retry    │ │ Save     │
    │ Next Step │ │ Agent    │ │ Version  │
    └───────────┘ └──────────┘ └────┬─────┘
                                    │
                                    ↓
                        ┌──────────────────────┐
                        │  GitHub Commit       │
                        │  (if modified)       │
                        └──────────────────────┘
```

## 6. Version Management Integration

### 6.1 GitHub Integration Flow

```python
class VersionManager:
    async def save_version(self, workflow_id: str, artifact: Artifact):
        # Get or create repository
        repo = await self.get_or_create_repo(workflow_id)
        
        # Prepare commit
        commit_message = f"{artifact.type} - {artifact.status}"
        
        # Create file structure
        file_path = f"artifacts/{artifact.type}.json"
        file_content = json.dumps(artifact.content, indent=2)
        
        # Commit to GitHub
        commit = await github_client.create_commit(
            repo=repo,
            branch="main",
            message=commit_message,
            files={file_path: file_content}
        )
        
        # Update artifact with commit info
        artifact.github_commit = {
            "sha": commit.sha,
            "url": commit.url
        }
        
        # Store latest version reference
        await self.update_latest_version(workflow_id, commit.sha)
        
        return commit
```

### 6.2 Version Retrieval for Next Agent

```python
async def get_latest_version(workflow_id: str) -> dict:
    # Get latest commit SHA
    latest_commit = await version_db.get_latest(workflow_id)
    
    # Fetch all artifacts from GitHub
    artifacts = await github_client.get_tree(
        repo=workflow_id,
        sha=latest_commit.sha
    )
    
    # Parse artifacts
    parsed_artifacts = {}
    for file in artifacts:
        if file.path.startswith("artifacts/"):
            artifact_type = file.path.replace("artifacts/", "").replace(".json", "")
            content = await github_client.get_file_content(
                repo=workflow_id,
                path=file.path,
                sha=latest_commit.sha
            )
            parsed_artifacts[artifact_type] = json.loads(content)
    
    return parsed_artifacts
```

## 7. Error Handling & Retry Logic

### 7.1 Agent Timeout Handling

```python
async def execute_agent_with_timeout(agent, input, timeout=300):
    try:
        result = await asyncio.wait_for(
            agent.process(input),
            timeout=timeout
        )
        return result
    except asyncio.TimeoutError:
        # Publish timeout event
        await websocket.publish(
            f"workflow.{input.workflowId}.error",
            {
                "step": agent.type,
                "error": "timeout",
                "retryable": True
            }
        )
        
        # Queue retry
        await queue.retry_job(agent, input, delay=60)
        
        raise AgentTimeoutError(f"Agent {agent.type} timed out")
```

### 7.2 Retry Strategy

```python
class RetryManager:
    MAX_RETRIES = 3
    BACKOFF_BASE = 2  # Exponential backoff
    
    async def retry_agent(self, agent, input, attempt=1):
        if attempt > self.MAX_RETRIES:
            # Move to manual intervention
            await self.trigger_manual_intervention(agent, input)
            return
        
        delay = self.BACKOFF_BASE ** attempt * 60  # 2min, 4min, 8min
        
        await asyncio.sleep(delay)
        
        try:
            result = await agent.process(input)
            return result
        except Exception as e:
            if e.retryable:
                return await self.retry_agent(agent, input, attempt + 1)
            else:
                raise
```

## 8. Parallel vs Sequential Execution

### 8.1 Current Design: Sequential
- **Rationale**: Each artifact depends on previous ones
- **Example**: User Stories need WBS, Tech Spec needs User Stories

### 8.2 Potential Parallelization
Some agents can run in parallel after certain checkpoints:

```
After WBS Validation:
├── User Stories Agent ──┐
├── Tech Spec Agent ─────┤─── Wait for both ──→ Next Step
└── NFR Agent ───────────┘
```

### 8.3 Conditional Parallel Execution

```python
class ParallelOrchestrator:
    async def execute_parallel_group(self, agents, workflow_state):
        # Execute all agents in parallel
        tasks = [
            self.execute_agent(agent, workflow_state)
            for agent in agents
        ]
        
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        # Handle results
        artifacts = []
        for result in results:
            if isinstance(result, Exception):
                await self.handle_error(result)
            else:
                artifacts.append(result.artifact)
        
        # All must be validated before proceeding
        await self.wait_for_all_validations(artifacts)
        
        return artifacts
```

## 9. State Persistence

### 9.1 Workflow State Schema

```sql
CREATE TABLE workflow_state (
    id UUID PRIMARY KEY,
    workflow_id UUID NOT NULL,
    current_step VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL, -- pending, in_progress, completed, failed
    progress INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    metadata JSONB
);

CREATE TABLE workflow_steps (
    id UUID PRIMARY KEY,
    workflow_id UUID NOT NULL,
    step_name VARCHAR(50) NOT NULL,
    agent_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    validated_at TIMESTAMP,
    validation_status VARCHAR(20), -- pending, approved, rejected, modified
    artifact_id UUID,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0
);
```

### 9.2 State Recovery

```python
async def recover_workflow(workflow_id: str):
    # Load workflow state from DB
    state = await db.get_workflow_state(workflow_id)
    
    # Check current step status
    if state.status == "in_progress":
        # Check if agent is still running
        agent_job = await queue.get_job(state.current_step_job_id)
        
        if agent_job is None or agent_job.failed:
            # Resume from last checkpoint
            await self.resume_from_step(state.current_step)
        else:
            # Agent still running, just monitor
            await self.monitor_agent(agent_job)
    elif state.status == "pending_validation":
        # Re-trigger validation UI
        await websocket.publish_validation_required(workflow_id)
```

## 10. Monitoring & Observability

### 10.1 Metrics to Track

- Agent execution time per type
- Validation time per step
- Timeout rate per agent
- Retry rate per agent
- Workflow completion rate
- Average workflow duration
- Error rate by agent type

### 10.2 Logging Strategy

```python
logger.info("workflow.started", {
    "workflow_id": workflow_id,
    "requirement_id": requirement_id
})

logger.info("agent.started", {
    "workflow_id": workflow_id,
    "agent_type": agent.type,
    "step": step_name
})

logger.info("agent.completed", {
    "workflow_id": workflow_id,
    "agent_type": agent.type,
    "duration": duration,
    "tokens_used": tokens
})

logger.info("validation.required", {
    "workflow_id": workflow_id,
    "artifact_type": artifact.type
})

logger.info("workflow.completed", {
    "workflow_id": workflow_id,
    "total_duration": duration,
    "steps_completed": step_count
})
```
