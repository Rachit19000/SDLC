# Complete End-to-End Workflow Documentation

## Overview
This document describes the complete workflow from frontend user interaction through backend processing to database storage, including authentication and authorization mechanisms.

---

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Authentication Flow (GitHub OAuth)](#authentication-flow-github-oauth)
3. [Authorization Mechanism](#authorization-mechanism)
4. [Document Upload Workflow](#document-upload-workflow)
5. [Database Integration](#database-integration)
6. [Real-Time Progress Updates](#real-time-progress-updates)
7. [Error Handling](#error-handling)

---

## Architecture Overview

```
┌─────────────┐
│   Frontend  │  React App (localhost:3000)
│  (React)    │
└──────┬──────┘
       │ HTTP/SSE
       ↓
┌─────────────────────────────────────┐
│      Backend Java (Spring Boot)     │  localhost:3001/api/v1
│  ┌───────────────────────────────┐  │
│  │  Controllers                  │  │
│  │  - AuthController             │  │
│  │  - RequirementsController     │  │
│  │  - JobController              │  │
│  │  - GitHubController           │  │
│  └───────────┬───────────────────┘  │
│              ↓                       │
│  ┌───────────────────────────────┐  │
│  │  Services                     │  │
│  │  - GitHubOAuthService         │  │
│  │  - TokenStore (In-Memory)      │  │
│  │  - DocumentParserService       │  │
│  │  - DocumentIngestionService    │  │
│  │  - JobService                  │  │
│  │  - UserGitHubUploadService     │  │
│  └───────────┬───────────────────┘  │
└──────────────┼───────────────────────┘
               │
       ┌───────┴────────┐
       ↓                ↓
┌──────────────┐  ┌──────────────┐
│  PostgreSQL  │  │   GitHub     │
│  Database    │  │     API       │
│  (sdlc_db)   │  │               │
└──────────────┘  └──────────────┘
```

---

## Authentication Flow (GitHub OAuth)

### Step-by-Step OAuth Flow

#### 1. **User Initiates Login** (Frontend)
**File:** `frontend/src/components/Login.js`

```javascript
// User clicks "Sign in with GitHub"
handleGitHubSignIn() {
  window.location.href = `${BACKEND_URL}/auth/github`;
}
```

**Flow:**
- Frontend redirects browser to: `http://localhost:3001/api/v1/auth/github`
- No token required (public endpoint)

---

#### 2. **Backend Initiates OAuth** (Backend)
**File:** `backend-java/src/main/java/com/sdlc/controller/AuthController.java`

**Endpoint:** `GET /api/v1/auth/github`

**Process:**
1. Generate random `state` token (CSRF protection)
2. Store state in memory with timestamp
3. Build GitHub OAuth URL with:
   - `client_id` (from `application.properties`)
   - `redirect_uri` (backend callback URL)
   - `scope` (read:user, user:email, repo)
   - `state` (CSRF token)
4. Redirect browser to GitHub authorization page

**Code:**
```java
String state = UUID.randomUUID().toString();
stateStore.put(state, System.currentTimeMillis());
String authorizationUrl = gitHubOAuthService.buildAuthorizationUrl(state);
response.sendRedirect(authorizationUrl);
```

---

#### 3. **User Authorizes on GitHub**
- User sees GitHub login page
- User enters credentials
- User grants permissions (read:user, user:email, repo)
- GitHub redirects back to: `http://localhost:3001/api/v1/auth/github/callback?code=...&state=...`

---

#### 4. **Backend Handles OAuth Callback** (Backend)
**File:** `backend-java/src/main/java/com/sdlc/controller/AuthController.java`

**Endpoint:** `GET /api/v1/auth/github/callback`

**Process:**
1. **Validate State** (CSRF protection)
   ```java
   if (!stateStore.containsKey(state)) {
       // Reject - possible CSRF attack
   }
   stateStore.remove(state); // One-time use
   ```

2. **Exchange Code for Access Token**
   ```java
   String accessToken = gitHubOAuthService.exchangeCodeForToken(code);
   ```
   - Calls GitHub API: `POST https://github.com/login/oauth/access_token`
   - Sends: `client_id`, `client_secret`, `code`
   - Receives: `access_token`

3. **Fetch User Info**
   ```java
   GitHubUserInfo userInfo = gitHubOAuthService.fetchGitHubUser(accessToken);
   ```
   - Calls GitHub API: `GET https://api.github.com/user`
   - Receives: `id`, `login`, `name`, `email`, `avatar_url`

4. **Create Session Token**
   ```java
   String sessionToken = "ghsession_" + userInfo.getGithubId() + "_" + System.currentTimeMillis();
   ```

5. **Store Token Mapping** (In-Memory)
   ```java
   tokenStore.store(sessionToken, new TokenStore.TokenData(
       accessToken,        // GitHub OAuth token (NEVER sent to frontend)
       userId,
       userInfo.getEmail(),
       userInfo.getName(),
       userInfo.getLogin(),
       userInfo.getAvatarUrl()
   ));
   ```

6. **Redirect to Frontend with Session Token**
   ```java
   String redirectUrl = frontendUrl + "/oauth/callback"
       + "?token=" + sessionToken      // Session token (safe for frontend)
       + "&name=" + userInfo.getName()
       + "&email=" + userInfo.getEmail()
       + "&githubUsername=" + userInfo.getLogin();
   response.sendRedirect(redirectUrl);
   ```

**Security Note:** The GitHub access token is **NEVER** sent to the frontend. Only a session token is sent.

---

#### 5. **Frontend Receives Session Token** (Frontend)
**File:** `frontend/src/components/OAuthCallback.js`

**Process:**
1. Extract token from URL query params
2. Store in localStorage:
   ```javascript
   localStorage.setItem('auth_token', token);
   localStorage.setItem('user', JSON.stringify({
       id: id,
       name: name,
       email: email,
       githubUsername: githubUsername
   }));
   ```
3. Redirect to dashboard: `navigate('/dashboard')`

---

## Authorization Mechanism

### How Authorization Works

#### 1. **Token Storage (Backend)**
**File:** `backend-java/src/main/java/com/sdlc/service/TokenStore.java`

- **Type:** In-memory `ConcurrentHashMap`
- **Key:** Session token (e.g., `ghsession_123456_1234567890`)
- **Value:** `TokenData` object containing:
  - `githubAccessToken` (GitHub OAuth token)
  - `userId`
  - `email`
  - `name`
  - `githubUsername`
  - `avatarUrl`

**Security:**
- GitHub access token is stored server-side only
- Never logged or exposed to frontend
- In production, use Redis or encrypted database

---

#### 2. **Token Validation (Backend)**

**Pattern:** All protected endpoints extract and validate token

**Example:** `RequirementsController.extractAndValidateToken()`

```java
private TokenStore.TokenData extractAndValidateToken(String authHeader) {
    // 1. Check Authorization header exists
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new RuntimeException("Authentication token required");
    }
    
    // 2. Extract token
    String token = authHeader.substring(7); // Remove "Bearer "
    
    // 3. Lookup in TokenStore
    TokenStore.TokenData tokenData = tokenStore.get(token);
    
    if (tokenData == null) {
        throw new RuntimeException("Invalid or expired session");
    }
    
    // 4. Verify GitHub token exists
    if (tokenData.getGithubAccessToken() == null) {
        throw new RuntimeException("GitHub access token not available");
    }
    
    return tokenData;
}
```

---

#### 3. **Frontend Token Usage**

**Pattern:** Include token in all API requests

```javascript
const token = localStorage.getItem('auth_token');

fetch(`${BACKEND_URL}/requirements/upload`, {
    method: 'POST',
    headers: {
        'Authorization': `Bearer ${token}`,
        // ... other headers
    },
    body: formData
});
```

---

#### 4. **Authorization Flow Diagram**

```
Frontend Request
    │
    │ Authorization: Bearer ghsession_123_456
    ↓
Backend Controller
    │
    │ extractAndValidateToken()
    ↓
TokenStore.get(token)
    │
    ├─→ Found: Return TokenData (with GitHub access token)
    │
    └─→ Not Found: Throw 401 Unauthorized
```

---

## Document Upload Workflow

### Complete Flow: File Upload to GitHub

#### 1. **User Selects File** (Frontend)
**File:** `frontend/src/components/RequirementUpload.js`

- User selects repository from dropdown
- User drags/drops file or selects via file input
- Supported formats: PDF, DOCX, TXT
- Frontend validates file type client-side

---

#### 2. **Frontend Sends Upload Request**
**File:** `frontend/src/components/RequirementUpload.js`

```javascript
const formData = new FormData();
formData.append('file', file);
formData.append('name', file.name);
formData.append('repoOwner', selectedRepo.owner);
formData.append('repoName', selectedRepo.name);

const response = await fetch(`${BACKEND_URL}/requirements/upload`, {
    method: 'POST',
    headers: {
        'Authorization': `Bearer ${token}`,
    },
    body: formData
});
```

---

#### 3. **Backend Receives Request** (Backend)
**File:** `backend-java/src/main/java/com/sdlc/controller/RequirementsController.java`

**Endpoint:** `POST /api/v1/requirements/upload`

**Process:**

**A. Authentication & Authorization**
```java
TokenStore.TokenData tokenData = extractAndValidateToken(authHeader);
// Returns: { githubAccessToken, userId, name, email, githubUsername }
```

**B. Validation**
- Check file is not empty
- Check file type is supported (PDF, DOCX, TXT)
- Validate `repoOwner` and `repoName` are provided

**C. Save File Temporarily**
```java
Path tempFile = Files.createTempFile("sdlc_upload_", "_" + fileName);
file.transferTo(tempFile);
```

**D. Create Async Job**
```java
Job job = jobService.createJob(fileName);
// Returns: { jobId: "job_1234567890_5678", status: "PROCESSING" }
```

**E. Start Async Processing**
```java
documentIngestionService.processDocumentAsync(
    job.getJobId(),
    tempFile,
    fileName,
    mimeType,
    tokenData.getGithubAccessToken(),  // User's GitHub token
    repoOwner,
    repoName,
    tokenData.getUserId()
);
```

**F. Return Immediately**
```java
return ResponseEntity.accepted().body(Map.of(
    "jobId", job.getJobId(),
    "status", "PROCESSING",
    "message", "Document upload accepted. Processing started.",
    "targetRepo", repoOwner + "/" + repoName
));
```

**Note:** Request returns immediately (202 Accepted). Processing happens asynchronously.

---

#### 4. **Async Document Processing** (Backend)
**File:** `backend-java/src/main/java/com/sdlc/service/DocumentIngestionService.java`

**Method:** `processDocumentAsync()` (runs in separate thread)

**Stages:**

**Stage 1: Initializing (0%)**
```java
jobService.updateProgress(jobId, 0, "Initializing");
Thread.sleep(500); // Give frontend time to connect SSE
```

**Stage 2: Detecting File Type (10%)**
```java
jobService.updateProgress(jobId, 10, "Detecting file type");
```

**Stage 3: Parsing Document (25%)**
```java
jobService.updateProgress(jobId, 25, "Parsing document");
byte[] fileBytes = Files.readAllBytes(tempFilePath);
String extractedText = parseFile(fileBytes, originalFileName, mimeType);
```

**Parsing Logic:**
- **PDF:** Uses Apache PDFBox `PDFTextStripper`
- **DOCX:** Uses Apache POI `XWPFWordExtractor`
- **TXT:** Direct UTF-8 read

**Stage 4: Text Extraction Complete (50%)**
```java
extractedText = cleanText(extractedText); // Normalize line breaks
jobService.updateProgress(jobId, 50, "Text extracted (" + extractedText.length() + " characters)");
```

**Stage 5: Preparing for Upload (65%)**
```java
String filePath = "requirements/" + jobId + "/" + parsedFileName;
String commitMessage = "Add parsed requirement: " + originalFileName;
```

**Stage 6: Uploading to GitHub (75%)**
```java
UserGitHubUploadService.UploadResult result = userGitHubUploadService.uploadFile(
    githubAccessToken,  // User's token from TokenStore
    repoOwner,
    repoName,
    filePath,
    extractedText,
    commitMessage
);
```

**GitHub Upload Process:**
1. Check if file exists (get SHA for update)
2. Base64 encode content
3. Call GitHub API: `PUT /repos/{owner}/{repo}/contents/{path}`
4. Include: `message`, `content` (base64), `sha` (if updating)
5. Receive: `fileUrl`, `commitUrl`, `sha`

**Stage 7: Finalizing (90%)**
```java
jobService.updateProgress(jobId, 90, "Finalizing");
```

**Stage 8: Complete (100%)**
```java
jobService.completeJob(jobId, result.getFileUrl());
```

**Cleanup:**
```java
finally {
    Files.deleteIfExists(tempFilePath); // Always clean up temp files
}
```

---

#### 5. **Real-Time Progress Updates** (SSE)

**Frontend Connects to SSE:**
**File:** `frontend/src/components/RequirementUpload.js`

```javascript
const eventSource = new EventSource(
    `${BACKEND_URL}/jobs/${jobId}/progress`
);

eventSource.addEventListener('progress', (event) => {
    const data = JSON.parse(event.data);
    setProgress(data.progress);
    setStage(data.stage);
    
    if (data.status === 'COMPLETED') {
        eventSource.close();
        // Show success message with GitHub file URL
    }
});
```

**Backend SSE Endpoint:**
**File:** `backend-java/src/main/java/com/sdlc/controller/JobController.java`

**Endpoint:** `GET /api/v1/jobs/{jobId}/progress`

**Process:**
1. Create `SseEmitter` with 5-minute timeout
2. Store emitter in `JobService.emitters` map
3. Send current job state immediately
4. As job progresses, emit events:
   ```java
   emitter.send(SseEmitter.event()
       .name("progress")
       .data({
           "jobId": jobId,
           "progress": 75,
           "stage": "Uploading to GitHub",
           "status": "PROCESSING"
       }));
   ```

**Job Service Updates:**
**File:** `backend-java/src/main/java/com/sdlc/service/JobService.java`

```java
public void updateProgress(String jobId, int progress, String stage) {
    Job job = jobs.get(jobId);
    job.setProgress(progress);
    job.setStage(stage);
    emitEvent(jobId, job); // Sends SSE event to connected client
}
```

---

## Database Integration

### Current Database Schema

**Database:** PostgreSQL (`sdlc_db`)

#### 1. **Settings Table**
```sql
CREATE TABLE settings (
    id SERIAL PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    description VARCHAR(500)
);
```

**Default Data:**
- `polling_time` = '5000' (milliseconds)
- `model_selected` = 'gpt-4'

**Usage:**
- Store application configuration
- Key-value pairs for settings
- Can be queried/updated via API

---

#### 2. **Job Mapping Table**
```sql
CREATE TABLE job_mapping (
    id SERIAL PRIMARY KEY,
    job_id VARCHAR(100) NOT NULL UNIQUE,
    project_id VARCHAR(100) NOT NULL,
    agent_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Usage:**
- Maps JobID to ProjectID and AgentID
- Tracks which agent processed which job
- Links jobs to projects

**Indexes:**
- `idx_job_mapping_job_id` (unique)
- `idx_job_mapping_project_id`
- `idx_job_mapping_agent_id`
- `idx_job_mapping_project_agent` (composite)

---

### Database Connection (Future)

**Configuration:** `application.properties`

```properties
# PostgreSQL Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/sdlc_db
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

**Note:** Currently, the application uses in-memory storage (`TokenStore`, `JobService`). Database integration is prepared but not yet implemented.

---

## Real-Time Progress Updates

### Server-Sent Events (SSE) Flow

```
Frontend                    Backend                    Job Processing
    │                           │                              │
    │  GET /jobs/{id}/progress │                              │
    ├──────────────────────────>│                              │
    │                           │ createEmitter(jobId)        │
    │                           │ Store emitter in map        │
    │                           │                              │
    │  SSE: progress event      │                              │
    │<──────────────────────────│                              │
    │                           │                              │
    │                           │                              │ updateProgress(50, "Parsing")
    │                           │                              ├─────────────────┐
    │                           │                              │                 │
    │                           │ emitEvent(jobId, job)       │                 │
    │                           │<────────────────────────────┘                 │
    │                           │                              │                 │
    │  SSE: progress event      │                              │                 │
    │<──────────────────────────│                              │                 │
    │                           │                              │                 │
    │                           │                              │ completeJob()   │
    │                           │                              ├─────────────────┐
    │                           │                              │                 │
    │                           │ emitEvent() + closeEmitter() │                 │
    │                           │<────────────────────────────┘                 │
    │                           │                              │                 │
    │  SSE: progress (COMPLETED)│                              │                 │
    │<──────────────────────────│                              │                 │
    │                           │                              │                 │
    │ Close SSE connection      │                              │                 │
    │──────────────────────────>│                              │                 │
```

---

## Error Handling

### Authentication Errors

**401 Unauthorized:**
- Missing `Authorization` header
- Invalid session token
- Expired GitHub access token

**Response:**
```json
{
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Invalid or expired session. Please re-authenticate with GitHub."
  }
}
```

**Frontend Handling:**
```javascript
if (response.status === 401) {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('user');
    navigate('/', { replace: true });
}
```

---

### Processing Errors

**Job Failure:**
```java
jobService.failJob(jobId, "GitHub token expired. Please re-authenticate.");
```

**SSE Event:**
```json
{
  "jobId": "job_123",
  "status": "FAILED",
  "error": "GitHub token expired. Please re-authenticate.",
  "progress": 75,
  "stage": "Failed"
}
```

**Frontend Handling:**
```javascript
if (data.status === 'FAILED') {
    eventSource.close();
    setError(data.error || 'Processing failed');
}
```

---

### File Validation Errors

**400 Bad Request:**
- Empty file
- Unsupported file type
- Missing `repoOwner` or `repoName`

**Response:**
```json
{
  "error": {
    "code": "BAD_REQUEST",
    "message": "Unsupported file type: image/png. Supported: PDF, DOCX, TXT"
  }
}
```

---

## Summary: Complete Request Flow

### Example: Document Upload

```
1. User clicks "Upload" in frontend
   ↓
2. Frontend: POST /requirements/upload
   Headers: Authorization: Bearer ghsession_123_456
   Body: FormData (file, repoOwner, repoName)
   ↓
3. Backend: RequirementsController.uploadFile()
   - Extract token from header
   - Validate token in TokenStore
   - Get GitHub access token from TokenStore
   - Validate file
   - Save to temp file
   - Create job
   - Start async processing
   - Return 202 Accepted with jobId
   ↓
4. Frontend: Connect to SSE /jobs/{jobId}/progress
   ↓
5. Backend: DocumentIngestionService.processDocumentAsync()
   - Parse document (PDF/DOCX/TXT)
   - Extract text
   - Upload to GitHub using user's token
   - Update progress via JobService
   - Emit SSE events
   ↓
6. Frontend: Receive SSE events
   - Update progress bar
   - Show stage
   - Display completion/error
   ↓
7. Backend: Complete job
   - Emit final SSE event
   - Close SSE connection
   - Clean up temp file
```

---

## Security Considerations

### 1. **Token Storage**
- GitHub access tokens stored server-side only
- Session tokens sent to frontend (safe)
- In-memory storage (production: use Redis/DB)

### 2. **CSRF Protection**
- OAuth state parameter
- One-time use
- Timestamp validation

### 3. **File Security**
- File type validation
- File size limits (10MB)
- Temp files always cleaned up
- Content sanitization

### 4. **Authorization**
- All protected endpoints require token
- Token validated on every request
- GitHub token used only for GitHub API calls

---

## API Endpoints Summary

### Public Endpoints
- `GET /api/v1/auth/github` - Initiate OAuth
- `GET /api/v1/auth/github/callback` - OAuth callback
- `GET /api/v1/auth/health` - Health check

### Protected Endpoints (Require Bearer Token)
- `POST /api/v1/requirements/upload` - Upload document
- `POST /api/v1/requirements/paste` - Paste text
- `GET /api/v1/jobs/{jobId}` - Get job status
- `GET /api/v1/jobs/{jobId}/progress` - SSE progress stream
- `GET /api/v1/github/repos` - List user repos
- `POST /api/v1/github/repos` - Create repo

---

## Technology Stack

- **Frontend:** React, React Router
- **Backend:** Spring Boot (Java)
- **Database:** PostgreSQL
- **External APIs:** GitHub OAuth, GitHub Contents API
- **Real-Time:** Server-Sent Events (SSE)
- **File Parsing:** Apache PDFBox, Apache POI

---

## Future Enhancements

1. **Database Integration:**
   - Store jobs in PostgreSQL
   - Store user sessions in database
   - Store settings in database

2. **JWT Tokens:**
   - Replace session tokens with JWT
   - Add refresh tokens
   - Token expiration handling

3. **Agent Orchestration:**
   - WBS generation agent
   - User stories agent
   - Technical specs agent

4. **Workflow Management:**
   - Multi-step workflows
   - Validation checkpoints
   - Version history

---

**Last Updated:** 2024
**Version:** 1.0
