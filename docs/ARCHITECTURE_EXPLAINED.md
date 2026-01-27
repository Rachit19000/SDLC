# AI-Driven SDLC Platform - Complete Architecture Explained

## What is This System?

Imagine you have a document with requirements for building a software project (like "We need an e-commerce website"). Instead of manually creating all the planning documents, this system uses AI to automatically generate:

1. **Work Breakdown Structure (WBS)** - A list of all tasks needed
2. **User Stories** - What users want and why
3. **Technical Specifications** - How to build it technically
4. **Architecture Diagrams** - How the system will be structured
5. **Test Plans** - How to test everything
6. **Sprint Plans** - When to build what
7. **Workspace Structure** - Folder organization for coding

**The Key Feature**: After each AI generates something, a human reviews it, can modify it, and only then does the system move to the next step. Everything is saved to GitHub automatically.

---

## The Big Picture: How It All Works Together

```
┌─────────────────────────────────────────────────────────────────┐
│                    YOU (The User)                                │
│  - Upload a PDF/DOCX file with requirements                      │
│  - OR paste text directly                                        │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        │ HTTP Request
                        ↓
┌─────────────────────────────────────────────────────────────────┐
│                    FRONTEND (Web Interface)                      │
│  - Shows you a nice website                                      │
│  - Lets you upload files                                         │
│  - Shows real-time progress                                      │
│  - Lets you review and approve/modify AI outputs                 │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        │ REST API Calls + WebSocket
                        ↓
┌─────────────────────────────────────────────────────────────────┐
│                    API GATEWAY                                   │
│  - Checks if you're logged in (Authentication)                   │
│  - Routes your requests to the right service                     │
│  - Limits how many requests you can make (Rate Limiting)         │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        │ Routes to appropriate service
                        ↓
┌─────────────────────────────────────────────────────────────────┐
│                    ORCHESTRATOR SERVICE                          │
│  - The "Conductor" of the whole process                          │
│  - Decides which AI agent to run next                            │
│  - Keeps track of where we are in the workflow                  │
│  - Waits for your approval before moving forward                │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        │ Creates job in queue
                        ↓
┌─────────────────────────────────────────────────────────────────┐
│                    REDIS QUEUE (Job Queue)                       │
│  - Like a to-do list for AI agents                               │
│  - Stores jobs waiting to be processed                           │
│  - Handles retries if something fails                           │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        │ Picks up job
                        ↓
┌─────────────────────────────────────────────────────────────────┐
│                    AI AGENT POOL                                 │
│  - Different specialized AI agents:                               │
│    • Document Parser - Reads your file                           │
│    • WBS Agent - Creates task breakdown                          │
│    • User Stories Agent - Creates user stories                   │
│    • Tech Spec Agent - Creates technical specs                   │
│    • And 6 more agents...                                        │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        │ Calls AI API (OpenAI/Claude)
                        ↓
┌─────────────────────────────────────────────────────────────────┐
│                    AI PROVIDER (OpenAI/Claude)                   │
│  - The actual AI that generates content                          │
│  - Takes prompts and returns generated text                      │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        │ Returns generated content
                        ↓
┌─────────────────────────────────────────────────────────────────┐
│                    ARTIFACT MANAGER                              │
│  - Saves the AI-generated content                                │
│  - Stores in database (PostgreSQL)                               │
│  - Stores large files in S3 (cloud storage)                     │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        │ Triggers validation
                        ↓
┌─────────────────────────────────────────────────────────────────┐
│                    VALIDATION SERVICE                           │
│  - Sends notification to frontend                                │
│  - Waits for your approval/modification                          │
│  - If you modify, saves to GitHub                               │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        │ If approved, triggers next agent
                        ↓
                    (Back to Orchestrator)
```

---

## Step-by-Step: What Happens When You Upload a Requirement

### Step 1: You Upload a File
```
You → Frontend → API Gateway → Orchestrator
```
- You drag and drop a PDF file or paste text
- Frontend sends it to the backend
- API Gateway checks you're logged in
- Orchestrator creates a new "workflow" (a process instance)

**What gets stored:**
- Your file goes to S3 (cloud storage)
- Metadata (file name, size, etc.) goes to PostgreSQL database
- A workflow ID is created to track everything

### Step 2: Document Parsing
```
Orchestrator → Queue → Document Parser Agent → AI (if needed) → Storage
```
- Orchestrator puts a job in the Redis queue: "Parse this document"
- Document Parser Agent picks up the job
- It reads the PDF/DOCX and extracts text
- Text is cleaned and stored in the database
- **Status**: "Ready for your review"

**Real-time update:**
- WebSocket sends message to frontend: "Document parsed!"
- You see a notification

### Step 3: You Review the Parsed Text
```
Frontend → Validation Service → Database
```
- Frontend shows you the extracted text
- You can:
  - **Approve**: "Looks good, continue"
  - **Modify**: Edit the text, then approve
  - **Reject**: "This is wrong, try again"
- Your decision is saved to the database

**If you modified:**
- The modified text is saved to GitHub as a commit
- Version history is updated

### Step 4: WBS Generation
```
Orchestrator → Queue → WBS Agent → OpenAI/Claude → Storage
```
- Orchestrator sees you approved the parsed text
- Creates a job: "Generate WBS from this requirement"
- WBS Agent picks up the job
- Agent prepares a prompt like:
  ```
  "Based on this requirement: [your text]
   Create a Work Breakdown Structure with tasks, subtasks, 
   dependencies, and time estimates."
  ```
- Sends prompt to OpenAI/Claude
- AI returns structured WBS (like a task list)
- Agent formats it and saves to database
- **Status**: "Ready for your review"

**Real-time update:**
- WebSocket: "WBS generated! Please review."

### Step 5: You Review the WBS
```
Frontend → Validation Service → (If modified) → GitHub
```
- Frontend shows you the WBS in an editable table
- You can:
  - **Approve**: Continue to next step
  - **Modify**: Add/remove tasks, change estimates
  - **Reject**: Regenerate (goes back to Step 4)

**If you modified:**
- Modified WBS is committed to GitHub
- Next agent will use this modified version

### Step 6: User Stories Generation
```
Orchestrator → Queue → User Stories Agent → AI → Storage
```
- Orchestrator triggers User Stories Agent
- Agent gets:
  - Original requirement text
  - The WBS you approved (from GitHub - latest version)
- Creates prompt:
  ```
  "Based on this requirement and WBS:
   Create user stories in format:
   - Epic: [Epic Name]
     - Story: As a [user], I want [feature] so that [benefit]"
  ```
- AI generates stories
- Saved to database
- **Status**: "Ready for your review"

### Steps 7-14: Repeat for Other Artifacts
The same pattern continues for:
- Technical Specifications
- Non-Functional Requirements (performance, security, etc.)
- Architecture Diagram
- Sprint Plan
- Test Scenarios
- Performance Test Plan
- Workspace Structure

Each step:
1. Agent generates content
2. You review and approve/modify
3. If modified, saved to GitHub
4. Next agent uses latest version from GitHub

### Step 15: Complete!
All artifacts are generated and validated. You have:
- Complete project documentation
- Everything saved in GitHub
- Ready for development

---

## Detailed Component Explanations

### 1. Frontend (Next.js Application)

**What it is:**
A web application that runs in your browser.

**What it does:**
- **File Upload Page**: Lets you drag-drop or paste requirements
- **Dashboard**: Shows all your projects and workflows
- **Progress Tracker**: Real-time updates via WebSocket
  - "Document parsing... 50%"
  - "WBS generation complete!"
- **Validation UI**: For each artifact
  - Shows the AI-generated content
  - Lets you edit it
  - Buttons: Approve / Modify / Reject
- **Artifact Viewers**: 
  - WBS as an editable table
  - User Stories as cards
  - Architecture as a diagram
  - Test cases as a spreadsheet

**How it communicates:**
- **REST API**: For actions (upload, approve, etc.)
- **WebSocket**: For real-time updates (no page refresh needed)

**Example Flow:**
```
1. You click "Upload File"
2. Frontend sends POST /requirements/upload
3. Gets back: { jobId: "abc123", status: "processing" }
4. Opens WebSocket connection
5. Receives: { type: "workflow.progress", progress: 25 }
6. Updates progress bar on screen
```

---

### 2. API Gateway

**What it is:**
A single entry point for all requests. Like a receptionist at a building.

**What it does:**
- **Authentication**: Checks if you're logged in
  - Every request includes a JWT token (like an ID card)
  - If no token or invalid token → "401 Unauthorized"
- **Routing**: Sends requests to the right service
  - `/requirements/*` → Orchestrator Service
  - `/artifacts/*` → Artifact Manager
  - `/validation/*` → Validation Service
- **Rate Limiting**: Prevents abuse
  - "You've made 100 requests this minute, wait a bit"
- **WebSocket Upgrade**: Converts HTTP to WebSocket for real-time updates

**Why it exists:**
- Single point of control
- Can add security, logging, monitoring in one place
- Can scale services independently

---

### 3. Orchestrator Service

**What it is:**
The "brain" that coordinates everything. Like a project manager.

**What it does:**

**A. Workflow Management**
- Creates a workflow when you upload a requirement
- Tracks current step: "We're at step 3 of 10"
- Stores workflow state in database

**B. Agent Coordination**
- Knows the sequence: Parser → WBS → Stories → Tech Spec → ...
- After each agent completes, triggers the next one
- But only after you approve!

**C. State Management**
- Keeps track of:
  - Which step we're on
  - What's been completed
  - What's waiting for approval
  - Any errors that occurred

**D. Event Publishing**
- When something happens, publishes events:
  - "Workflow started"
  - "Agent completed"
  - "Validation required"
- These events go to WebSocket, which notifies frontend

**Example Internal Flow:**
```python
# Pseudo-code of what Orchestrator does

1. Receive: "Start workflow for requirement_123"
2. Create workflow record in database
3. Create first job: "Parse document"
4. Put job in Redis queue
5. Wait...
6. Receive: "Document parsed, artifact_id: 456"
7. Update workflow: current_step = "parsed_text_ready"
8. Publish WebSocket event: "Validation required"
9. Wait for validation...
10. Receive: "User approved parsed_text"
11. Create next job: "Generate WBS"
12. Put job in queue
13. Repeat...
```

---

### 4. Redis Queue (Job Queue)

**What it is:**
A temporary storage for jobs waiting to be processed. Like a to-do list.

**Why we need it:**
- AI agents can take 30 seconds to 5 minutes
- We can't make the user wait that long
- So we:
  1. Put job in queue
  2. Return immediately: "Job queued, we'll process it"
  3. Process in background
  4. Notify via WebSocket when done

**How it works:**
```
Job Structure:
{
  id: "job_123",
  type: "wbs_generation",
  workflow_id: "wf_456",
  input: {
    requirement_text: "...",
    previous_artifacts: {...}
  },
  status: "queued" | "processing" | "completed" | "failed"
}
```

**Agent picks up job:**
1. Agent Pool Manager checks queue
2. Finds job with status "queued"
3. Changes status to "processing"
4. Gives job to an available agent
5. Agent processes it
6. Updates status to "completed" or "failed"

**Retry Logic:**
- If agent fails (timeout, error), job goes back to queue
- Retry up to 3 times
- If still fails, goes to "dead letter queue" for manual review

---

### 5. AI Agent Pool

**What it is:**
A collection of specialized AI agents. Each agent does one specific task.

**Types of Agents:**

**A. Document Parser Agent**
- **Input**: PDF/DOCX file
- **Process**: 
  - Uses library (like PyPDF2) to extract text
  - Cleans the text (removes formatting artifacts)
  - Structures it
- **Output**: Clean text ready for AI processing
- **No AI needed**: Just file parsing

**B. WBS Agent**
- **Input**: Requirement text
- **Process**:
  - Creates prompt: "Generate WBS from this requirement..."
  - Sends to OpenAI/Claude
  - Receives generated WBS
  - Parses and structures it (JSON format)
- **Output**: Structured WBS with tasks, dependencies, estimates

**C. User Stories Agent**
- **Input**: Requirement text + WBS (from previous step)
- **Process**:
  - Creates prompt: "Generate user stories based on requirement and WBS..."
  - Sends to AI
  - Parses response into structured format
- **Output**: Epics and User Stories

**D. Tech Spec Agent**
- **Input**: Requirement + WBS + User Stories
- **Process**: Similar to above
- **Output**: Technical specifications

**E. Other Agents**: NFR, Architecture, Sprint Plan, Test Scenarios, Performance Test, Workspace

**Agent Structure:**
```python
class WBSAgent:
    async def process(self, input):
        # 1. Prepare prompt
        prompt = self.create_prompt(input.requirement_text)
        
        # 2. Call AI
        response = await openai.generate(prompt)
        
        # 3. Parse response
        wbs = self.parse_response(response)
        
        # 4. Return structured output
        return {
            type: "wbs",
            content: wbs,
            metadata: {
                model: "gpt-4",
                tokens_used: 1500,
                processing_time: 45
            }
        }
```

**Agent Pool Manager:**
- Manages multiple instances of each agent
- Load balancing: Distributes jobs across available agents
- Health monitoring: Checks if agents are running
- Timeout handling: If agent takes too long, cancels and retries

---

### 6. AI Provider (OpenAI/Claude)

**What it is:**
The actual AI service that generates content.

**How it works:**
- Agent sends a prompt (text instruction)
- AI processes it and generates response
- Returns generated text
- Agent parses and structures it

**Example Prompt:**
```
You are an expert software architect. Based on the following requirement:

"Build an e-commerce platform with user authentication, product catalog, 
shopping cart, and payment processing."

Generate a Work Breakdown Structure with:
- High-level tasks
- Subtasks for each
- Dependencies between tasks
- Estimated hours for each task
- Priority (High/Medium/Low)

Format as JSON.
```

**AI Response:**
```json
{
  "tasks": [
    {
      "id": "task_1",
      "name": "User Authentication",
      "subtasks": [
        {"name": "Login page", "hours": 8, "priority": "High"},
        {"name": "Registration", "hours": 6, "priority": "High"}
      ],
      "dependencies": []
    },
    ...
  ]
}
```

**Why we use AI:**
- Can understand natural language requirements
- Generates structured, professional documentation
- Adapts to different types of projects
- Learns from patterns in training data

---

### 7. Artifact Manager

**What it is:**
Service that stores and retrieves all AI-generated content.

**What it stores:**
- **Small content** (< 100KB): Directly in PostgreSQL database
- **Large content** (> 100KB): In S3, reference in database

**Storage Structure:**
```
PostgreSQL (artifacts table):
- id: "artifact_123"
- workflow_id: "wf_456"
- artifact_type: "wbs"
- content: {JSON data} OR null if large
- file_url: null OR "s3://bucket/artifacts/wf_456/wbs.json"
- version: 1
- status: "ready" | "validated" | "modified"
- created_at: timestamp

S3 (if large):
- Path: artifacts/wf_456/wbs.json
- Content: Full JSON file
```

**What it does:**
- **Store**: Saves artifact when agent completes
- **Retrieve**: Gets artifact when you want to view/edit
- **Version**: Tracks different versions (original, modified, etc.)
- **Cache**: Stores frequently accessed artifacts in Redis for speed

---

### 8. Validation Service

**What it is:**
Manages the human review process.

**What it does:**

**A. Validation Checkpoint**
- After each agent completes, creates a validation checkpoint
- Sends notification to frontend via WebSocket
- Waits for your decision

**B. Handle Your Decision**
- **Approve**: 
  - Marks artifact as "validated"
  - Updates workflow state
  - Triggers next agent
- **Modify**:
  - Saves your modifications
  - Creates new version
  - Commits to GitHub (via Version Manager)
  - Marks as "validated"
  - Triggers next agent
- **Reject**:
  - Marks artifact as "rejected"
  - Can trigger retry (regenerate)
  - Or manual intervention

**C. Version Tracking**
- Records who validated, when, and what action
- Stores modification history
- Links to GitHub commits

**Example Flow:**
```
1. Agent completes → Artifact saved
2. Validation Service creates checkpoint
3. Publishes WebSocket event: "Validation required for WBS"
4. Frontend shows validation UI
5. You click "Approve"
6. Frontend sends: POST /validation/validate
   {
     workflow_id: "wf_123",
     artifact_type: "wbs",
     action: "approve"
   }
7. Validation Service:
   - Updates artifact status
   - Records validation
   - Notifies Orchestrator: "WBS approved"
8. Orchestrator triggers next agent
```

---

### 9. Version Manager (GitHub Integration)

**What it is:**
Service that manages versions using GitHub.

**Why GitHub:**
- Don't reinvent version control
- GitHub is battle-tested
- Provides version history, diffs, etc.
- Can integrate with existing tools

**What it does:**

**A. Repository Creation**
- When workflow starts, creates a GitHub repository
- Sets up folder structure:
  ```
  artifacts/
    parsed_text.json
    wbs.json
    user_stories.json
    ...
  ```

**B. Commit on Validation**
- When you modify an artifact:
  1. Validation Service calls Version Manager
  2. Version Manager:
     - Gets modified artifact content
     - Creates commit message: "WBS - Modified by user"
     - Commits to GitHub
     - Gets commit SHA (unique ID)
  3. Stores commit info in database

**C. Version Retrieval**
- When next agent needs previous artifacts:
  1. Agent requests: "Get latest version"
  2. Version Manager:
     - Gets latest commit SHA from database
     - Fetches all files from GitHub at that commit
     - Returns artifacts to agent
  3. Agent uses these as input

**Example:**
```
Step 1: WBS generated → Saved to DB
Step 2: You modify WBS → Committed to GitHub (commit_abc123)
Step 3: User Stories Agent starts
        → Requests: "Get latest artifacts"
        → Version Manager fetches from GitHub at commit_abc123
        → Returns: { wbs: {...modified version...} }
        → Agent uses modified WBS to generate stories
```

**Benefits:**
- Full version history
- Can see what changed when
- Can rollback if needed
- Integrates with existing Git workflows

---

### 10. Database (PostgreSQL)

**What it stores:**

**A. User Data**
- Users table: email, password hash, name
- Projects table: project name, owner, description

**B. Workflow Data**
- Workflows table: workflow ID, status, current step, progress
- Workflow steps table: Each step's status, start/end time, errors

**C. Artifact Data**
- Artifacts table: Content, type, version, status
- Validations table: Who validated, when, what action

**D. Version History**
- Links to GitHub commits
- Tracks which artifacts are in which commit

**Why PostgreSQL:**
- Reliable, ACID compliant
- Handles complex queries
- JSON support (for artifact content)
- Good for structured data

---

### 11. Redis

**What it stores:**

**A. Job Queue**
- Jobs waiting to be processed
- Job status (queued, processing, completed)

**B. Workflow State (Fast Access)**
- Current status of active workflows
- Updated frequently, accessed often
- Faster than database queries

**C. WebSocket Sessions**
- Which users are connected to which workflows
- For sending real-time updates

**D. Cache**
- Frequently accessed artifacts
- Reduces database load

**Why Redis:**
- Very fast (in-memory)
- Good for queues
- Pub/Sub for real-time events
- Temporary data (can be rebuilt from PostgreSQL)

---

### 12. S3/MinIO (File Storage)

**What it stores:**
- Original requirement files (PDF/DOCX)
- Large artifact files (> 100KB)
- Generated diagrams/images

**Why separate storage:**
- Database is for structured data
- Files are binary/large
- S3 is optimized for file storage
- Can serve files directly via CDN

---

## Data Flow: Complete Example

Let's trace a complete example from start to finish:

### Scenario: You upload "Build an e-commerce platform" requirement

**Step 1: Upload**
```
You → Frontend
  → POST /requirements/upload (file: ecommerce.pdf)
  → API Gateway (checks auth)
  → Orchestrator
    → Saves file to S3: s3://bucket/requirements/req_123.pdf
    → Creates workflow record in PostgreSQL
    → Creates job in Redis queue: { type: "parse", file: "req_123.pdf" }
    → Returns: { jobId: "job_456", status: "processing" }
  → Frontend receives response
  → Opens WebSocket connection
```

**Step 2: Document Parsing**
```
Document Parser Agent
  → Picks up job from Redis queue
  → Downloads file from S3
  → Extracts text using PDF parser
  → Cleans text
  → Saves to PostgreSQL:
     {
       workflow_id: "wf_789",
       artifact_type: "parsed_text",
       content: "Build an e-commerce platform with user authentication..."
     }
  → Updates Redis: workflow state = "parsed_text_ready"
  → Publishes WebSocket event: "workflow.step.complete"
  → Frontend receives event, shows notification
```

**Step 3: Your Review**
```
You → Frontend
  → Views parsed text
  → Clicks "Approve"
  → POST /validation/validate
    {
      workflow_id: "wf_789",
      artifact_type: "parsed_text",
      action: "approve"
    }
  → Validation Service
    → Updates artifact status in PostgreSQL
    → Records validation
    → Notifies Orchestrator: "parsed_text approved"
  → Orchestrator
    → Creates next job: { type: "wbs_generation", requirement_text: "..." }
    → Puts in Redis queue
```

**Step 4: WBS Generation**
```
WBS Agent
  → Picks up job
  → Prepares prompt:
     "Generate WBS for: Build an e-commerce platform..."
  → Calls OpenAI API
  → Receives response:
     {
       "tasks": [
         {"name": "User Auth", "hours": 16, ...},
         {"name": "Product Catalog", "hours": 24, ...}
       ]
     }
  → Structures and saves to PostgreSQL
  → Updates Redis state
  → Publishes WebSocket: "WBS ready for validation"
  → Frontend shows WBS in editable table
```

**Step 5: You Modify WBS**
```
You → Frontend
  → Edits WBS table (adds "Security Audit" task)
  → Clicks "Approve with Modifications"
  → POST /validation/validate
    {
      workflow_id: "wf_789",
      artifact_type: "wbs",
      action: "modify",
      modifications: { ...modified WBS... }
    }
  → Validation Service
    → Updates artifact in PostgreSQL (version 2)
    → Calls Version Manager
  → Version Manager
    → Creates GitHub commit:
       Message: "WBS - Modified by user"
       Files: artifacts/wbs.json (with your changes)
    → Gets commit SHA: "abc123def456"
    → Saves to PostgreSQL: version_history table
  → Validation Service
    → Marks as validated
    → Notifies Orchestrator: "WBS validated"
  → Orchestrator
    → Creates next job: "Generate User Stories"
    → Job includes: "Get latest artifacts from GitHub"
```

**Step 6: User Stories Generation**
```
User Stories Agent
  → Picks up job
  → Requests: "Get latest artifacts"
  → Version Manager
    → Gets latest commit SHA from PostgreSQL: "abc123def456"
    → Fetches from GitHub:
       GET /repos/org/project/git/trees/abc123def456
    → Returns: { wbs: {...your modified WBS with Security Audit...} }
  → Agent prepares prompt:
     "Generate user stories based on requirement and this WBS:
      [includes your Security Audit task]"
  → Calls OpenAI
  → Generates stories (including security-related ones)
  → Saves to PostgreSQL
  → Publishes WebSocket: "User Stories ready"
```

**And so on...** until all artifacts are generated.

---

## Why This Architecture?

### 1. Asynchronous Processing
**Problem**: AI calls take 30s-5min. Can't make user wait.
**Solution**: Queue-based processing. Return immediately, process in background, notify when done.

### 2. Human Validation
**Problem**: AI might make mistakes or miss requirements.
**Solution**: Checkpoint after each step. Human reviews, can modify, then continues.

### 3. Version Management
**Problem**: Need to track changes, especially when human modifies.
**Solution**: Use GitHub. Each modification = commit. Next agent uses latest version.

### 4. Real-time Updates
**Problem**: User wants to see progress without refreshing.
**Solution**: WebSocket. Server pushes updates to frontend automatically.

### 5. Scalability
**Problem**: Many users, many workflows running simultaneously.
**Solution**: 
- Stateless services (can run multiple instances)
- Queue distributes work
- Database and Redis can be clustered

### 6. Resilience
**Problem**: Things fail (network, AI timeout, etc.)
**Solution**:
- Retry logic (exponential backoff)
- Dead letter queue for failed jobs
- State recovery from database

---

## Technology Choices Explained

### Frontend: Next.js
- **Why**: React framework, server-side rendering, good performance
- **What it gives**: Fast page loads, good SEO, easy to build UI

### Backend: Node.js or Python
- **Why**: 
  - Node.js: JavaScript everywhere, good async support
  - Python: Great AI libraries, FastAPI is fast
- **What it gives**: Easy to build APIs, good ecosystem

### Database: PostgreSQL
- **Why**: Reliable, handles complex queries, JSON support
- **What it gives**: Structured storage, relationships, transactions

### Queue: Redis
- **Why**: Fast (in-memory), built-in pub/sub, good for queues
- **What it gives**: Job queue, caching, real-time events

### Storage: S3/MinIO
- **Why**: Optimized for files, scalable, CDN integration
- **What it gives**: File storage separate from database

### AI: OpenAI/Claude
- **Why**: Best language models, good APIs
- **What it gives**: High-quality content generation

### Version Control: GitHub API
- **Why**: Don't reinvent, battle-tested, integrates with existing tools
- **What it gives**: Version history, diffs, rollback capability

---

## Summary

This system automates the creation of SDLC artifacts using AI, but keeps humans in the loop for quality control. It's built as microservices that communicate via APIs and events, processes work asynchronously through queues, and maintains versions using GitHub.

The key innovation is the **sequential agent workflow with human validation checkpoints**, ensuring quality while automating the tedious documentation work.
