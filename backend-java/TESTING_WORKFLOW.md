# Complete Workflow Testing Guide

## Overview
This guide tests the complete workflow: **Upload Document → Parse Text → Generate User Stories**

---

## Prerequisites

### 1. MCP Host Configuration
Ensure your MCP host is running and accessible. Update `application.properties`:

```properties
mcp.host.url=http://localhost:8080
```

Or set environment variable:
```powershell
$env:MCP_HOST_URL="http://your-mcp-host:port"
```

### 2. MCP Server Setup
Your MCP server should have a `user_story_agent` that:
- Accepts POST requests to `/execute`
- Expects JSON body:
  ```json
  {
    "agent": "user_story_agent",
    "arguments": {
      "requirement_text": "...",
      "job_id": "..."
    }
  }
  ```
- Returns JSON response:
  ```json
  {
    "status": "success",
    "metadata": {
      "user_stories": "...generated user stories text..."
    }
  }
  ```

### 3. Backend Running
```powershell
cd backend-java
mvn spring-boot:run
```

### 4. Frontend Running
```powershell
cd frontend
npm start
```

---

## Testing Steps

### Step 1: Start Backend
```powershell
cd backend-java
mvn spring-boot:run
```

**Expected Output:**
```
Started SdlcBackendApplication in X.XXX seconds
```

**Verify:**
- Backend is accessible at: `http://localhost:3001/api/v1/auth/health`
- Should return: `{"status":"ok","message":"Backend API is running"}`

---

### Step 2: Configure MCP Host URL

**Option A: Environment Variable**
```powershell
$env:MCP_HOST_URL="http://localhost:8080"
```

**Option B: application.properties**
```properties
mcp.host.url=http://localhost:8080
```

**Verify MCP Host:**
- Test if MCP host is accessible
- Ensure the `/execute` endpoint exists
- Verify agent `user_story_agent` is registered

---

### Step 3: Upload Document via Frontend

1. **Open Frontend:** `http://localhost:3000`
2. **Sign in with GitHub** (if not already signed in)
3. **Navigate to Upload Page**
4. **Select Repository** from dropdown
5. **Upload a PDF or Word file** containing requirements

**Example Test Document Content:**
```
Project Requirements:
- Build an e-commerce platform
- User authentication system
- Product catalog with search
- Shopping cart functionality
- Payment processing
- Order management
```

---

### Step 4: Monitor Progress

**Frontend:**
- Watch the progress bar
- Expected stages:
  1. Initializing (0%)
  2. Detecting file type (10%)
  3. Parsing document (25%)
  4. Text extracted (50%)
  5. **Generating user stories (60%)** ← NEW
  6. User stories generated (70%)
  7. Preparing for upload (75%)
  8. Uploading parsed text (80%)
  9. Uploading user stories (85%) ← NEW
  10. Finalizing (95%)
  11. Completed (100%)

**Backend Logs:**
```
Parsing document...
Text extracted: X characters
Calling user story agent for job: job_123456
Calling MCP host at: http://localhost:8080/execute
User stories generated successfully for job: job_123456
Uploading parsed text to GitHub...
Uploading user stories to GitHub...
Job completed
```

---

### Step 5: Verify Results

**Check GitHub Repository:**

1. **Parsed Text File:**
   - Path: `requirements/{jobId}/{filename}_parsed.md`
   - Contains: Extracted requirement text

2. **User Stories File:**
   - Path: `requirements/{jobId}/{filename}_user_stories.md`
   - Contains: Generated user stories in markdown format

**Expected User Stories Format:**
```markdown
# User Stories

## Epic 1: User Authentication
- **Story 1:** As a user, I want to register an account so that I can access the platform
- **Story 2:** As a user, I want to login so that I can access my account

## Epic 2: Product Catalog
- **Story 1:** As a customer, I want to browse products so that I can find items to purchase
- **Story 2:** As a customer, I want to search products so that I can find specific items
...
```

---

## Troubleshooting

### Issue 1: "MCP host URL not configured"
**Solution:**
- Add `mcp.host.url` to `application.properties`
- Or set `MCP_HOST_URL` environment variable
- Restart backend

---

### Issue 2: "Failed to call MCP agent"
**Check:**
1. MCP host is running
2. MCP host URL is correct
3. Agent `user_story_agent` exists
4. Network connectivity to MCP host

**Test MCP Host:**
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/execute" -Method POST -ContentType "application/json" -Body '{"agent":"user_story_agent","arguments":{"requirement_text":"test","job_id":"test"}}'
```

---

### Issue 3: "User stories not generated"
**Possible Causes:**
- MCP agent failed (check backend logs)
- Agent returned error status
- Network timeout

**Workflow Continues:**
- Even if user story generation fails, the parsed text is still uploaded
- Check backend logs for specific error

---

### Issue 4: "User stories uploaded but empty"
**Check:**
- MCP agent response format
- Ensure `metadata.user_stories` or `metadata.result` contains the stories
- Check backend logs for response structure

---

## API Testing (Manual)

### Test Document Upload
```powershell
$token = "your_auth_token"
$filePath = "path/to/test.pdf"

$formData = @{
    file = Get-Item $filePath
    repoOwner = "your_github_username"
    repoName = "your_repo_name"
    name = "Test Requirements"
}

$response = Invoke-RestMethod -Uri "http://localhost:3001/api/v1/requirements/upload" `
    -Method POST `
    -Headers @{Authorization="Bearer $token"} `
    -Form $formData

$jobId = $response.jobId
Write-Host "Job ID: $jobId"
```

### Monitor Job Progress
```powershell
# Poll job status
while ($true) {
    $status = Invoke-RestMethod -Uri "http://localhost:3001/api/v1/jobs/$jobId" `
        -Headers @{Authorization="Bearer $token"}
    
    Write-Host "Status: $($status.status) - Progress: $($status.progress)% - Stage: $($status.stage)"
    
    if ($status.status -eq "COMPLETED" -or $status.status -eq "FAILED") {
        break
    }
    
    Start-Sleep -Seconds 2
}
```

---

## Expected Workflow Flow

```
1. User uploads document
   ↓
2. Backend receives file
   ↓
3. Document parsed (PDF/DOCX → Text)
   ↓
4. Text cleaned and normalized
   ↓
5. MCP Agent called: generateUserStories(parsedText, jobId)
   ↓
6. MCP host executes user_story_agent
   ↓
7. User stories generated
   ↓
8. Parsed text uploaded to GitHub
   ↓
9. User stories uploaded to GitHub
   ↓
10. Job completed
```

---

## Success Criteria

✅ **Document parsing works:**
- PDF/DOCX files are parsed correctly
- Text is extracted and cleaned

✅ **MCP agent integration works:**
- Agent is called after parsing
- User stories are generated
- No errors in backend logs

✅ **GitHub upload works:**
- Both parsed text and user stories are uploaded
- Files are accessible in GitHub repository
- Commit messages are correct

✅ **Progress tracking works:**
- Frontend shows all stages
- Progress updates in real-time
- Job completes successfully

---

## Next Steps

After successful testing:
1. Verify user stories quality
2. Test with different document types
3. Test with larger documents
4. Add more agents (WBS, Tech Spec, etc.)
5. Implement validation checkpoints

---

**Last Updated:** 2024
