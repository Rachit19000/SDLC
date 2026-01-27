# System Component Interaction Diagrams

## 1. Sequence Diagram: Complete Workflow

```
User          Frontend        API Gateway    Orchestrator    Agent Pool    Agent      Validation    GitHub
  │               │                │              │              │           │            │           │
  │──Upload File─>│                │              │              │           │            │           │
  │               │──POST /upload─>│              │              │           │            │           │
  │               │                │──Forward────>│              │           │            │           │
  │               │                │              │──Create Job──>│          │            │           │
  │               │                │              │              │           │            │           │
  │               │<──202 Accepted─│<──Job ID─────│              │           │            │           │
  │<──Job ID──────│                │              │              │           │            │           │
  │               │                │              │              │           │            │           │
  │               │                │              │              │──Process─>│           │           │
  │               │                │              │              │           │──AI API───>│           │
  │               │                │              │              │           │<──Result───│           │
  │               │                │              │              │<──Result──│           │           │
  │               │                │              │<──Complete───│           │            │           │
  │               │                │              │──Store───────│           │            │           │
  │               │                │              │──WebSocket──>│           │            │           │
  │<──WebSocket───│                │              │              │           │            │           │
  │               │                │              │              │           │            │           │
  │──Validate────>│                │              │              │           │            │           │
  │               │──POST /validate>│              │              │           │            │           │
  │               │                │──Forward────>│              │           │            │           │
  │               │                │              │──Validate───>│           │            │           │
  │               │                │              │              │           │            │           │
  │               │                │              │              │           │            │──Commit───>│
  │               │                │              │              │           │            │<──SHA──────│
  │               │                │              │<──Validated──│           │            │           │
  │               │<──200 OK───────│<──Success────│              │           │            │           │
  │               │                │              │              │           │            │           │
  │               │                │              │──Next Agent─>│           │            │           │
  │               │                │              │              │──Process─>│           │            │
  │               │                │              │              │           │──Get Ver──>│           │
  │               │                │              │              │           │<──Artifacts│           │
  │               │                │              │              │           │──AI API───>│           │
  │               │                │              │              │           │<──Result───│           │
  │               │                │              │              │<──Result──│           │            │
  │               │                │              │              │           │            │           │
  │               │                │              │              │           │            │           │
  │<──Complete────│<──WebSocket────│<──Event──────│              │           │            │           │
```

## 2. Component Interaction: Agent Execution

```
Orchestrator          Agent Pool Manager      Agent Service      AI Provider      Queue (Redis)
     │                        │                    │                  │                  │
     │──Trigger Agent────────>│                    │                  │                  │
     │                        │──Select Agent──────>│                  │                  │
     │                        │                    │──Enqueue────────>│                  │
     │                        │                    │                  │                  │
     │                        │                    │<──Job Ready──────│                  │
     │                        │                    │──Process─────────>│                  │
     │                        │                    │                  │──API Call────────>│
     │                        │                    │                  │<──Response────────│
     │                        │                    │<──Result──────────│                  │
     │                        │<──Complete─────────│                  │                  │
     │<──Result───────────────│                    │                  │                  │
     │                        │                    │                  │                  │
     │──Store Artifact────────│                    │                  │                  │
     │──Publish Event─────────│                    │                  │                  │
```

## 3. Component Interaction: Validation Flow

```
Frontend          Validation Service      Artifact Manager      Version Manager      GitHub API
   │                      │                      │                     │                  │
   │──Validate Request───>│                      │                     │                  │
   │                      │──Get Artifact───────>│                     │                  │
   │                      │<──Artifact───────────│                     │                  │
   │                      │                      │                     │                  │
   │                      │──Check Action────────│                     │                  │
   │                      │                      │                     │                  │
   │                      │  [If Modified]       │                     │                  │
   │                      │                      │──Prepare Commit────>│                  │
   │                      │                      │                     │──Create Commit──>│
   │                      │                      │                     │<──Commit SHA─────│
   │                      │                      │<──Version Info──────│                  │
   │                      │<──Updated─────────────│                     │                  │
   │<──Validation Result──│                      │                     │                  │
   │                      │                      │                     │                  │
   │                      │──Trigger Next────────│                     │                  │
```

## 4. Component Interaction: WebSocket Communication

```
Frontend          WebSocket Server      Event Publisher      Redis Pub/Sub      Orchestrator
   │                      │                     │                  │                  │
   │──Connect─────────────>│                     │                  │                  │
   │<──Connected───────────│                     │                  │                  │
   │                      │                     │                  │                  │
   │                      │                     │                  │                  │
   │                      │                     │                  │                  │
   │                      │<──Agent Complete────│                  │                  │
   │                      │──Publish Event───────>│                  │                  │
   │                      │                     │──Pub to Channel──>│                  │
   │<──Event──────────────│<──Event─────────────│<──Event───────────│                  │
   │                      │                     │                  │                  │
   │                      │                     │                  │                  │
   │                      │<──Validation Req────│                  │                  │
   │                      │──Publish─────────────>│                  │                  │
   │<──Validation Req─────│<──Event─────────────│<──Event───────────│                  │
```

## 5. Component Interaction: Version Management

```
Agent              Version Manager      GitHub API         PostgreSQL        Orchestrator
  │                      │                   │                   │                  │
  │──Need Latest Ver────>│                   │                   │                  │
  │                      │──Get Latest SHA──>│                   │                  │
  │                      │                   │──Get Commit──────>│                  │
  │                      │                   │<──Commit Info─────│                  │
  │                      │──Get Tree─────────>│                   │                  │
  │                      │                   │──Get Files────────>│                  │
  │                      │                   │<──File Contents───│                  │
  │<──Artifacts───────────│                   │                   │                  │
  │                      │                   │                   │                  │
  │                      │                   │                   │                  │
  │──Process─────────────│                   │                   │                  │
  │──Complete────────────│                   │                   │                  │
  │                      │                   │                   │                  │
  │                      │<──Save Version─────│                   │                  │
  │                      │──Create Commit────>│                   │                  │
  │                      │                   │──Commit──────────>│                  │
  │                      │                   │<──Commit SHA──────│                  │
  │                      │──Store in DB───────>│                   │                  │
  │                      │                   │                   │──Update State───>│
```

## 6. System Boundary Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         EXTERNAL BOUNDARY                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌──────────────┐         ┌──────────────┐         ┌──────────────┐    │
│  │   Users      │         │  AI Provider  │         │   GitHub     │    │
│  │  (Browser)   │         │  (OpenAI/     │         │   API        │    │
│  │              │         │   Claude)     │         │              │    │
│  └──────┬───────┘         └──────┬───────┘         └──────┬───────┘    │
│         │                        │                        │            │
│         │ HTTP/WebSocket         │ API Calls              │ API Calls  │
│         │                        │                        │            │
└─────────┼────────────────────────┼────────────────────────┼────────────┘
          │                        │                        │
          │                        │                        │
┌─────────┼────────────────────────┼────────────────────────┼────────────┐
│         │                        │                        │            │
│  ┌──────▼───────┐         ┌──────▼───────┐         ┌──────▼───────┐   │
│  │   Frontend   │         │  Agent       │         │  Version     │   │
│  │   Service    │         │  Services    │         │  Manager     │   │
│  └──────┬───────┘         └──────┬───────┘         └──────┬───────┘   │
│         │                        │                        │            │
│  ┌──────▼────────────────────────▼────────────────────────▼───────┐   │
│  │                    Orchestrator Service                        │   │
│  └──────┬────────────────────────┬────────────────────────┬───────┘   │
│         │                        │                        │            │
│  ┌──────▼───────┐         ┌──────▼───────┐         ┌──────▼───────┐   │
│  │  PostgreSQL  │         │    Redis     │         │  S3/MinIO    │   │
│  │  (Metadata)  │         │  (Queue/     │         │  (Files)     │   │
│  │              │         │   Cache)     │         │              │   │
│  └──────────────┘         └──────────────┘         └──────────────┘   │
│                                                                           │
└─────────────────────────────────────────────────────────────────────────┘
```

## 7. Deployment Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        LOAD BALANCER                              │
└───────────────────────┬──────────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
┌───────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐
│  Frontend    │ │  Frontend    │ │  Frontend   │
│  Instance 1  │ │  Instance 2  │ │  Instance 3 │
└───────┬──────┘ └──────┬──────┘ └──────┬──────┘
        │               │               │
        └───────────────┼───────────────┘
                        │
┌───────────────────────▼──────────────────────────────────────────┐
│                        API GATEWAY                                │
└───────────────────────┬──────────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
┌───────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐
│ Orchestrator │ │ Orchestrator│ │ Orchestrator│
│  Service 1   │ │  Service 2  │ │  Service 3  │
└───────┬──────┘ └──────┬──────┘ └──────┬──────┘
        │               │               │
        └───────────────┼───────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
┌───────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐
│ Agent Pool 1 │ │ Agent Pool 2│ │ Agent Pool 3│
└───────┬──────┘ └──────┬──────┘ └──────┬──────┘
        │               │               │
        └───────────────┼───────────────┘
                        │
┌───────────────────────▼──────────────────────────────────────────┐
│                    SHARED SERVICES                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  PostgreSQL  │  │    Redis     │  │  S3/MinIO    │          │
│  │   (Primary)  │  │   (Cluster)  │  │   (Storage)  │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└──────────────────────────────────────────────────────────────────┘
```

## 8. Error Handling Flow

```
Agent Service      Orchestrator      Retry Manager      Dead Letter Queue
     │                  │                  │                    │
     │──Process────────>│                  │                    │
     │                  │                  │                    │
     │──Error───────────>│                  │                    │
     │                  │──Check Retry─────>│                    │
     │                  │                  │                    │
     │                  │  [Retryable]     │                    │
     │                  │<──Schedule───────│                    │
     │                  │                  │                    │
     │                  │                  │──Retry──────────────>│
     │                  │                  │                    │
     │                  │                  │──Max Retries───────>│
     │                  │                  │                    │
     │                  │<──Manual Trigger─│                    │
     │                  │                  │                    │
     │                  │──Notify User─────>│                    │
```

## 9. Key Interaction Patterns

### 9.1 Request-Response Pattern
- **Use Case**: Synchronous operations (validation, status checks)
- **Components**: Frontend ↔ API Gateway ↔ Services
- **Protocol**: HTTP REST

### 9.2 Publish-Subscribe Pattern
- **Use Case**: Real-time updates, workflow progress
- **Components**: Services → Redis Pub/Sub → WebSocket → Frontend
- **Protocol**: WebSocket over Redis

### 9.3 Queue-Based Pattern
- **Use Case**: Async agent processing
- **Components**: Orchestrator → Redis Queue → Agent Pool → Agent
- **Protocol**: Bull/BullMQ

### 9.4 Event-Driven Pattern
- **Use Case**: Workflow orchestration, state transitions
- **Components**: All services publish/subscribe to events
- **Protocol**: Redis Pub/Sub + WebSocket

## 10. Component Responsibilities Matrix

| Component | Primary Responsibility | Key Interactions |
|-----------|----------------------|------------------|
| Frontend | User interface, real-time updates | API Gateway, WebSocket |
| API Gateway | Routing, auth, rate limiting | All services |
| Orchestrator | Workflow management, coordination | All services |
| Agent Pool Manager | Agent lifecycle, load balancing | Orchestrator, Agents |
| Agent Services | AI processing, artifact generation | AI Provider, Queue |
| Validation Service | Human validation management | Artifact Manager, Version Manager |
| Artifact Manager | Artifact storage, retrieval | PostgreSQL, S3 |
| Version Manager | GitHub integration, versioning | GitHub API, PostgreSQL |
| Workspace Manager | Workspace structure generation | GitHub API, Version Manager |
