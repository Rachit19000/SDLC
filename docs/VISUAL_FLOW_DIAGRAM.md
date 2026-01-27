# Visual Flow Diagrams - Simple Step-by-Step

## Complete User Journey (Simple View)

```
┌─────────────────────────────────────────────────────────────┐
│                    YOU (The User)                            │
│                                                               │
│  📄 You have a requirement document                          │
│     "Build an e-commerce platform"                          │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        │ 1. Upload File
                        ↓
┌─────────────────────────────────────────────────────────────┐
│                    WEBSITE (Frontend)                       │
│                                                               │
│  📤 Drag & Drop File                                         │
│     OR                                                       │
│  📝 Paste Text                                               │
│                                                               │
│  [Upload Button] → Click                                    │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        │ 2. Send to Server
                        ↓
┌─────────────────────────────────────────────────────────────┐
│                    SERVER PROCESSES                         │
│                                                               │
│  Step A: Parse Document                                      │
│    └─→ Extracts text from PDF/DOCX                          │
│                                                               │
│  Step B: Generate WBS                                        │
│    └─→ AI creates task breakdown                             │
│                                                               │
│  Step C: Generate User Stories                               │
│    └─→ AI creates user stories                               │
│                                                               │
│  ... and 7 more steps                                        │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        │ 3. After Each Step
                        ↓
┌─────────────────────────────────────────────────────────────┐
│                    YOU REVIEW                                │
│                                                               │
│  👀 See what AI generated                                    │
│                                                               │
│  ✏️  Edit if needed                                          │
│                                                               │
│  ✅ Approve → Continue to next step                          │
│  ❌ Reject → Regenerate                                      │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        │ 4. Repeat for Each Step
                        ↓
┌─────────────────────────────────────────────────────────────┐
│                    FINAL RESULT                              │
│                                                               │
│  ✅ All artifacts generated                                  │
│  ✅ Everything saved to GitHub                               │
│  ✅ Ready for development                                    │
└─────────────────────────────────────────────────────────────┘
```

---

## Detailed Step-by-Step Flow

### Step 1: Upload Requirement

```
┌──────────┐
│   YOU    │
└────┬─────┘
     │
     │ "I want to upload this PDF"
     │
     ↓
┌─────────────────────────────────────┐
│         FRONTEND (Website)         │
│                                     │
│  [File Upload Area]                │
│  ┌─────────────────────────────┐   │
│  │  Drag file here or click     │   │
│  │  📄 ecommerce_requirements.pdf│   │
│  └─────────────────────────────┘   │
│                                     │
│  [Upload] ← You click              │
└────┬────────────────────────────────┘
     │
     │ HTTP POST /requirements/upload
     │
     ↓
┌─────────────────────────────────────┐
│         API GATEWAY                │
│                                     │
│  ✓ Check: Are you logged in?       │
│  ✓ Check: Is file valid?            │
│  ✓ Route to: Orchestrator           │
└────┬────────────────────────────────┘
     │
     ↓
┌─────────────────────────────────────┐
│      ORCHESTRATOR SERVICE           │
│                                     │
│  1. Save file to cloud storage      │
│  2. Create workflow record          │
│  3. Create job: "Parse document"    │
│  4. Put job in queue                │
│                                     │
│  Returns: { jobId: "123",           │
│             status: "processing" }   │
└────┬────────────────────────────────┘
     │
     │ Response back to you
     ↓
┌─────────────────────────────────────┐
│         FRONTEND                    │
│                                     │
│  Shows: "Processing... Job ID: 123" │
│  Opens: WebSocket connection        │
│         (for real-time updates)     │
└─────────────────────────────────────┘
```

---

### Step 2: Document Parsing (Background)

```
┌─────────────────────────────────────┐
│      REDIS QUEUE (Job List)         │
│                                     │
│  📋 Job #1: Parse document          │
│     Status: ⏳ Waiting              │
└────┬────────────────────────────────┘
     │
     │ Agent picks up job
     ↓
┌─────────────────────────────────────┐
│   DOCUMENT PARSER AGENT             │
│                                     │
│  1. Get file from storage           │
│  2. Read PDF/DOCX                   │
│  3. Extract text                    │
│  4. Clean text                      │
│  5. Save to database                │
│                                     │
│  Result: Clean text extracted      │
└────┬────────────────────────────────┘
     │
     │ Save result
     ↓
┌─────────────────────────────────────┐
│      DATABASE (PostgreSQL)          │
│                                     │
│  Artifact:                          │
│  - Type: "parsed_text"              │
│  - Content: "Build an e-commerce..."│
│  - Status: "ready_for_review"      │
└────┬────────────────────────────────┘
     │
     │ Send notification
     ↓
┌─────────────────────────────────────┐
│      WEBSOCKET (Real-time)          │
│                                     │
│  Message:                           │
│  "Document parsed! Please review."  │
└────┬────────────────────────────────┘
     │
     │ Push to your browser
     ↓
┌─────────────────────────────────────┐
│         FRONTEND                    │
│                                     │
│  🔔 Notification appears            │
│  "Document parsed - Click to view"│
└─────────────────────────────────────┘
```

---

### Step 3: You Review Parsed Text

```
┌─────────────────────────────────────┐
│         FRONTEND                    │
│                                     │
│  ┌───────────────────────────────┐ │
│  │  Parsed Text                   │ │
│  │  ───────────────────────────  │ │
│  │  Build an e-commerce platform  │ │
│  │  with user authentication,     │ │
│  │  product catalog, shopping     │ │
│  │  cart, and payment processing. │ │
│  └───────────────────────────────┘ │
│                                     │
│  [Edit] [Approve] [Reject]         │
└────┬────────────────────────────────┘
     │
     │ You click "Approve"
     │
     ↓
┌─────────────────────────────────────┐
│    VALIDATION SERVICE               │
│                                     │
│  1. Record your approval            │
│  2. Mark artifact as "validated"    │
│  3. Notify Orchestrator             │
│     "Parsed text approved"         │
└────┬────────────────────────────────┘
     │
     ↓
┌─────────────────────────────────────┐
│      ORCHESTRATOR                  │
│                                     │
│  "Great! Now generate WBS"          │
│                                     │
│  Creates job: "Generate WBS"        │
│  Puts in queue                      │
└─────────────────────────────────────┘
```

---

### Step 4: WBS Generation

```
┌─────────────────────────────────────┐
│      REDIS QUEUE                     │
│                                     │
│  📋 Job #2: Generate WBS            │
│     Status: ⏳ Waiting              │
└────┬────────────────────────────────┘
     │
     │ WBS Agent picks up
     ↓
┌─────────────────────────────────────┐
│         WBS AGENT                   │
│                                     │
│  1. Get requirement text            │
│  2. Create prompt:                  │
│     "Generate WBS for:              │
│      Build an e-commerce platform..."│
│  3. Send to AI                      │
└────┬────────────────────────────────┘
     │
     │ API Call
     ↓
┌─────────────────────────────────────┐
│      OPENAI/CLAUDE (AI)             │
│                                     │
│  Processes prompt...                │
│  Generates response...              │
│                                     │
│  Returns:                           │
│  {                                  │
│    "tasks": [                       │
│      {                              │
│        "name": "User Auth",         │
│        "hours": 16,                 │
│        "priority": "High"           │
│      },                             │
│      {                              │
│        "name": "Product Catalog",   │
│        "hours": 24,                 │
│        "priority": "High"           │
│      }                              │
│    ]                                │
│  }                                  │
└────┬────────────────────────────────┘
     │
     │ AI Response
     ↓
┌─────────────────────────────────────┐
│         WBS AGENT                   │
│                                     │
│  4. Parse AI response               │
│  5. Structure as JSON              │
│  6. Save to database                │
└────┬────────────────────────────────┘
     │
     │ Save result
     ↓
┌─────────────────────────────────────┐
│      DATABASE                       │
│                                     │
│  Artifact:                          │
│  - Type: "wbs"                      │
│  - Content: { tasks: [...] }      │
│  - Status: "ready_for_review"      │
└────┬────────────────────────────────┘
     │
     │ Notify via WebSocket
     ↓
┌─────────────────────────────────────┐
│         FRONTEND                    │
│                                     │
│  🔔 "WBS Generated! Please review"  │
└─────────────────────────────────────┘
```

---

### Step 5: You Review and Modify WBS

```
┌─────────────────────────────────────┐
│         FRONTEND                    │
│                                     │
│  ┌───────────────────────────────┐ │
│  │  Work Breakdown Structure     │ │
│  │  ───────────────────────────  │ │
│  │  Task 1: User Auth (16 hrs)  │ │
│  │  Task 2: Product Catalog (24)│ │
│  │  Task 3: Shopping Cart (20)    │ │
│  │  Task 4: Payment (18)          │ │
│  │                                │ │
│  │  [+ Add Task]                 │ │
│  └───────────────────────────────┘ │
│                                     │
│  You add: "Security Audit (8 hrs)"  │
│                                     │
│  [Approve with Modifications]       │
└────┬────────────────────────────────┘
     │
     │ POST /validation/validate
     │ { action: "modify", 
     │   modifications: {...} }
     ↓
┌─────────────────────────────────────┐
│    VALIDATION SERVICE               │
│                                     │
│  1. Save modified WBS               │
│  2. Create new version (v2)        │
│  3. Call Version Manager            │
└────┬────────────────────────────────┘
     │
     ↓
┌─────────────────────────────────────┐
│    VERSION MANAGER                  │
│                                     │
│  1. Get modified WBS                │
│  2. Create GitHub commit:           │
│     Message: "WBS - Modified"      │
│     File: artifacts/wbs.json       │
│  3. Get commit SHA: "abc123"        │
│  4. Save to database                │
└────┬────────────────────────────────┘
     │
     │ Commit created
     ↓
┌─────────────────────────────────────┐
│         GITHUB                      │
│                                     │
│  Repository: project-123           │
│  └─ artifacts/                     │
│     └─ wbs.json (your modified)    │
│                                     │
│  Commit: abc123def456              │
│  "WBS - Modified by user"          │
└─────────────────────────────────────┘
```

---

### Step 6: Next Agent Uses Your Modified Version

```
┌─────────────────────────────────────┐
│   USER STORIES AGENT                │
│                                     │
│  Needs: Previous artifacts          │
│                                     │
│  Requests: "Get latest version"     │
└────┬────────────────────────────────┘
     │
     ↓
┌─────────────────────────────────────┐
│    VERSION MANAGER                  │
│                                     │
│  1. Get latest commit SHA           │
│     from database: "abc123"         │
│  2. Fetch from GitHub:             │
│     GET /repos/.../git/trees/abc123 │
│  3. Returns:                        │
│     { wbs: {...your modified...} } │
└────┬────────────────────────────────┘
     │
     │ Gets your modified WBS
     ↓
┌─────────────────────────────────────┐
│   USER STORIES AGENT                │
│                                     │
│  Creates prompt:                    │
│  "Generate user stories based on:   │
│   - Requirement: ...                │
│   - WBS: [includes Security Audit] │
│                                     │
│  Note: Your Security Audit task     │
│  is included in the prompt!        │
└────┬────────────────────────────────┘
     │
     │ Sends to AI
     ↓
┌─────────────────────────────────────┐
│      OPENAI/CLAUDE                   │
│                                     │
│  Generates stories including:       │
│  - Security-related user stories   │
│    (because WBS had Security Audit)│
└─────────────────────────────────────┘
```

---

## Complete Workflow Timeline

```
Time    │ What Happens
────────┼─────────────────────────────────────────────
0:00    │ You upload requirement PDF
        │
0:01    │ Document Parser extracts text
        │ └─→ Status: "Ready for review"
        │
0:02    │ 🔔 You get notification
        │ 👀 You review parsed text
        │ ✅ You approve
        │
0:03    │ WBS Agent starts
        │ └─→ Calls OpenAI (takes 30-60 seconds)
        │
0:04    │ WBS generated
        │ └─→ Status: "Ready for review"
        │
0:05    │ 🔔 You get notification
        │ 👀 You review WBS
        │ ✏️  You add "Security Audit" task
        │ ✅ You approve with modifications
        │ └─→ Saved to GitHub (commit abc123)
        │
0:06    │ User Stories Agent starts
        │ └─→ Gets latest from GitHub (your modified WBS)
        │ └─→ Calls OpenAI
        │
0:07    │ User Stories generated
        │ └─→ Status: "Ready for review"
        │
0:08    │ 🔔 You get notification
        │ 👀 You review stories
        │ ✅ You approve
        │
0:09    │ Tech Spec Agent starts
        │ ... (continues for all 10 steps)
        │
15:00   │ ✅ All artifacts complete!
        │ 📦 Everything in GitHub
        │ 🚀 Ready for development
```

---

## Component Communication Flow

```
┌──────────┐
│  FRONTEND│
└────┬─────┘
     │
     │ HTTP REST API
     │ (for actions)
     ↓
┌──────────┐         ┌──────────┐
│   API    │────────▶│ORCHESTR │
│ GATEWAY  │         │  RATOR   │
└──────────┘         └────┬─────┘
     │                    │
     │                    │ Creates job
     │                    ↓
     │              ┌──────────┐
     │              │  REDIS   │
     │              │  QUEUE   │
     │              └────┬─────┘
     │                   │
     │                   │ Agent picks up
     │                   ↓
     │              ┌──────────┐
     │              │  AGENT   │
     │              │  POOL    │
     │              └────┬─────┘
     │                   │
     │                   │ Calls AI
     │                   ↓
     │              ┌──────────┐
     │              │   AI     │
     │              │ PROVIDER │
     │              └────┬─────┘
     │                   │
     │                   │ Returns result
     │                   ↓
     │              ┌──────────┐
     │              │ARTIFACT  │
     │              │ MANAGER  │
     │              └────┬─────┘
     │                   │
     │                   │ Saves to DB
     │                   ↓
     │              ┌──────────┐
     │              │DATABASE  │
     │              │(Postgres)│
     │              └──────────┘
     │
     │ WebSocket
     │ (for real-time updates)
     ↓
┌──────────┐
│  FRONTEND│
│  (gets   │
│  updates)│
└──────────┘
```

---

## Data Storage Locations

```
┌─────────────────────────────────────────┐
│         WHERE DATA IS STORED            │
├─────────────────────────────────────────┤
│                                         │
│  📁 PostgreSQL Database                 │
│  ──────────────────────────            │
│  • User accounts                        │
│  • Workflow status                      │
│  • Artifact metadata                    │
│  • Small artifacts (< 100KB)            │
│  • Validation history                   │
│                                         │
│  📦 S3/MinIO (File Storage)             │
│  ──────────────────────────            │
│  • Original requirement files           │
│  • Large artifacts (> 100KB)            │
│  • Generated diagrams                   │
│                                         │
│  ⚡ Redis (Fast Temporary Storage)      │
│  ──────────────────────────            │
│  • Job queue                            │
│  • Workflow state (active)              │
│  • Cache (frequently accessed)         │
│  • WebSocket sessions                   │
│                                         │
│  🐙 GitHub (Version Control)            │
│  ──────────────────────────            │
│  • All validated artifacts              │
│  • Version history                      │
│  • Modified versions                    │
│                                         │
└─────────────────────────────────────────┘
```

---

## Error Handling Flow

```
┌─────────────────────────────────────┐
│      AGENT PROCESSING               │
│                                     │
│  ⏱️  Timeout: 5 minutes            │
└────┬────────────────────────────────┘
     │
     │ If timeout or error
     ↓
┌─────────────────────────────────────┐
│      RETRY MANAGER                   │
│                                     │
│  Attempt 1: Wait 2 minutes          │
│  Attempt 2: Wait 4 minutes         │
│  Attempt 3: Wait 8 minutes          │
│                                     │
│  If all fail:                       │
└────┬────────────────────────────────┘
     │
     ↓
┌─────────────────────────────────────┐
│   DEAD LETTER QUEUE                 │
│                                     │
│  📋 Failed jobs                     │
│  └─→ Manual review needed          │
│                                     │
│  🔔 Notify admin                    │
└─────────────────────────────────────┘
```

---

## Summary: The Complete Picture

```
┌─────────────────────────────────────────────────────────────┐
│                    YOUR JOURNEY                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 📄 Upload requirement                                   │
│     └─→ File saved, workflow created                       │
│                                                             │
│  2. ⚙️  System processes (10 steps)                         │
│     ├─→ Parse document                                      │
│     ├─→ Generate WBS                                        │
│     ├─→ Generate User Stories                              │
│     ├─→ Generate Tech Spec                                 │
│     ├─→ Generate NFR                                       │
│     ├─→ Generate Architecture                              │
│     ├─→ Generate Sprint Plan                               │
│     ├─→ Generate Test Scenarios                            │
│     ├─→ Generate Performance Test                          │
│     └─→ Generate Workspace                                 │
│                                                             │
│  3. 👀 You review each step (10 times)                     │
│     ├─→ See what AI generated                              │
│     ├─→ Edit if needed                                     │
│     └─→ Approve to continue                                │
│                                                             │
│  4. 💾 Everything saved                                     │
│     ├─→ Database (metadata)                                │
│     ├─→ S3 (files)                                         │
│     └─→ GitHub (versions)                                  │
│                                                             │
│  5. ✅ Complete!                                            │
│     └─→ All artifacts ready for development                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

This is the complete flow in simple terms. The system does the heavy lifting (AI generation), but you stay in control (review and modify at each step).
