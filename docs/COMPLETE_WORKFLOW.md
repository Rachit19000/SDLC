# Complete Project Workflow

## Overview
This document explains the complete end-to-end workflow of the SDLC Automation Platform, from user login to document parsing and GitHub storage.

---

## Table of Contents
1. [Authentication Flow](#authentication-flow)
2. [File Upload Flow](#file-upload-flow)
3. [Text Paste Flow](#text-paste-flow)
4. [Document Parsing Process](#document-parsing-process)
5. [GitHub Storage Process](#github-storage-process)
6. [Complete Data Flow Diagram](#complete-data-flow-diagram)

---

## Authentication Flow

### Step 1: User Login
**File:** `frontend/src/components/Login.js`

1. User enters email and password
2. Form submission triggers `handleSubmit()` function
3. Frontend makes API call:

```javascript
POST http://localhost:3001/api/v1/auth/login
Headers: { 'Content-Type': 'application/json' }
Body: { email: 'rachitjainemail@gmail.com', password: 'password123' }
```

### Step 2: Backend Authentication
**File:** `backend/server.js` (lines 34-60)

1. Backend receives request at `/api/v1/auth/login`
2. Searches `mockUsers` array for matching email/password
3. If found:
   - Generates token: `mock_token_${user.id}_${Date.now()}`
   - Returns: `{ token, user: { id, email, name } }`
4. If not found:
   - Returns 401 error

### Step 3: Frontend Token Storage
**File:** `frontend/src/components/Login.js` (lines 38-44)

1. Frontend receives response
2. Stores token in `localStorage`: `auth_token`
3. Stores user data in `localStorage`: `user`
4. Redirects to `/dashboard`

---

## File Upload Flow

### Step 1: User Selects File
**File:** `frontend/src/components/RequirementUpload.js`

1. User clicks "Drag & Drop File" button
2. User selects file (PDF/DOCX/TXT) or drags & drops
3. File is stored in React state: `fileName`

### Step 2: User Clicks Upload
**File:** `frontend/src/components/RequirementUpload.js` (lines 54-102)

1. `handleUpload()` function is triggered
2. Gets token from `localStorage.getItem('auth_token')`
3. Creates `FormData` object:
   ```javascript
   formData.append('file', file);
   formData.append('name', file.name);
   ```

### Step 3: Frontend API Call
**File:** `frontend/src/components/RequirementUpload.js` (lines 72-78)

```javascript
POST http://localhost:3001/api/v1/requirements/upload
Headers: {
  'Authorization': `Bearer ${token}`
}
Body: FormData (multipart/form-data)
```

**Note:** No `Content-Type` header needed - browser sets it automatically for FormData

### Step 4: Backend Receives File
**File:** `backend/server.js` (lines 109-220)

1. **Multer Middleware** (`upload.single('file')`) processes the file:
   - Stores file in memory as Buffer
   - Attaches to `req.file` object
   - File properties: `originalname`, `mimetype`, `size`, `buffer`

2. **Authentication Check:**
   - Extracts token from `Authorization` header
   - Finds user from `mockUsers` array
   - Validates user exists

3. **File Validation:**
   - Checks file exists
   - Validates file type (PDF/DOCX/TXT)
   - Logs file details

### Step 5: Document Parsing
**File:** `backend/server.js` (lines 169-179) → `backend/document-parser.js`

1. Calls `parseDocument(req.file.buffer, mimeType, fileName)`

2. **Parser Logic** (`document-parser.js` lines 14-91):
   
   **For PDF files:**
   ```javascript
   const pdfData = await pdfParse(fileBuffer);
   extractedText = pdfData.text;
   metadata.pages = pdfData.numpages;
   ```
   - Uses `pdf-parse` library
   - Extracts text from all pages
   - Returns page count and document info

   **For DOCX files:**
   ```javascript
   const result = await mammoth.extractRawText({ buffer: fileBuffer });
   extractedText = result.value;
   ```
   - Uses `mammoth` library
   - Extracts raw text content
   - Handles formatting warnings

   **For TXT files:**
   ```javascript
   extractedText = fileBuffer.toString('utf-8');
   ```
   - Direct UTF-8 conversion

3. **Text Cleaning:**
   - Normalizes line breaks (`\r\n` → `\n`)
   - Removes excessive blank lines
   - Trims whitespace

4. Returns: `{ text: extractedText, metadata: {...} }`

### Step 6: GitHub Storage
**File:** `backend/server.js` (lines 181-201) → `backend/github-config.js`

1. **Generate File Path:**
   ```javascript
   jobId = `job_${Date.now()}`
   textFileName = `${originalName}_extracted.txt`
   filePath = `requirements/${user.id}/${jobId}/${textFileName}`
   ```
   Example: `requirements/user_1/job_1703123456789/document_extracted.txt`

2. **Create Commit Message:**
   ```
   "Add parsed requirement: document.pdf (Job: job_123) - 5 pages - 1234 characters extracted"
   ```

3. **Call GitHub API** (`github-config.js` lines 48-100):
   ```javascript
   createFileInGitHub(user.email, filePath, extractedText, commitMessage)
   ```

4. **GitHub API Process:**
   - Gets user's repo config from `userRepos` map
   - Encodes text to base64 (GitHub API requirement)
   - Checks if file exists (gets SHA if updating)
   - Calls GitHub API:
     ```javascript
     octokit.repos.createOrUpdateFileContents({
       owner: 'Rachit19000',
       repo: 'files_storage',
       path: filePath,
       message: commitMessage,
       content: base64EncodedText,
       branch: 'main'
     })
     ```

5. **GitHub Response:**
   - Returns file URL: `https://github.com/.../blob/main/requirements/...`
   - Returns commit URL: `https://github.com/.../commit/abc123`

### Step 7: Backend Response
**File:** `backend/server.js` (lines 207-220)

```json
{
  "jobId": "job_1703123456789",
  "status": "stored",
  "message": "File parsed and text uploaded successfully to GitHub",
  "githubUrl": "https://github.com/.../document_extracted.txt",
  "commitUrl": "https://github.com/.../commit/abc123",
  "filePath": "requirements/user_1/job_123/document_extracted.txt",
  "fileName": "document_extracted.txt",
  "originalFileName": "document.pdf",
  "extractedTextLength": 1234,
  "metadata": { "pages": 5, ... }
}
```

### Step 8: Frontend Success Display
**File:** `frontend/src/components/RequirementUpload.js` (lines 89-100)

1. Receives response
2. Displays success message:
   ```
   File parsed and uploaded to GitHub!
   Job ID: job_123
   Extracted 1234 characters
   View file: https://github.com/...
   ```
3. Clears file input

---

## Text Paste Flow

### Step 1: User Pastes Text
**File:** `frontend/src/components/RequirementUpload.js`

1. User clicks "Paste Text" button
2. User types/pastes text in textarea
3. Text stored in React state: `pastedText`

### Step 2: User Clicks Upload
**File:** `frontend/src/components/RequirementUpload.js` (lines 104-137)

1. `handleUpload()` function triggered
2. Gets token from `localStorage`
3. Validates text is not empty

### Step 3: Frontend API Call
**File:** `frontend/src/components/RequirementUpload.js` (lines 109-119)

```javascript
POST http://localhost:3001/api/v1/requirements/paste
Headers: {
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${token}`
}
Body: {
  text: "user's pasted text",
  name: "Pasted Requirements"
}
```

### Step 4: Backend Receives Text
**File:** `backend/server.js` (lines 188-269)

1. **Authentication Check:**
   - Extracts token from header
   - Validates user

2. **Text Validation:**
   - Checks text exists and is not empty

3. **No Parsing Needed** (text is already clean)

### Step 5: GitHub Storage
**File:** `backend/server.js` (lines 224-247)

1. **Generate File Path:**
   ```javascript
   jobId = `job_${Date.now()}`
   fileName = `requirement_${jobId}.txt`
   filePath = `requirements/${user.id}/${jobId}/${fileName}`
   ```

2. **Direct GitHub Upload:**
   - Text is already clean, no parsing needed
   - Calls `createFileInGitHub()` with text directly
   - Same GitHub API process as file upload

3. **Response:**
   ```json
   {
     "jobId": "job_123",
     "status": "stored",
     "githubUrl": "https://github.com/.../requirement_job_123.txt",
     ...
   }
   ```

---

## Document Parsing Process

### Parser Agent
**File:** `backend/document-parser.js`

### Supported Formats

| Format | Library | Function | Output |
|--------|---------|----------|--------|
| PDF | `pdf-parse` | `pdfParse(buffer)` | Text + page count |
| DOCX | `mammoth` | `mammoth.extractRawText()` | Raw text |
| TXT | Native | `buffer.toString('utf-8')` | Direct text |

### Parsing Steps

1. **File Type Detection:**
   - Checks `mimeType` (e.g., `application/pdf`)
   - Checks file extension (e.g., `.pdf`)

2. **Format-Specific Parsing:**
   - **PDF:** Extracts text from all pages, gets metadata
   - **DOCX:** Extracts raw text, handles warnings
   - **TXT:** Direct UTF-8 conversion

3. **Text Cleaning:**
   ```javascript
   extractedText
     .replace(/\r\n/g, '\n')      // Normalize line breaks
     .replace(/\r/g, '\n')        // Handle Mac line breaks
     .replace(/\n{3,}/g, '\n\n')  // Remove excessive blank lines
     .trim()                       // Remove leading/trailing whitespace
   ```

4. **Validation:**
   - Checks text is not empty
   - Throws error if extraction fails

5. **Return:**
   ```javascript
   {
     text: "cleaned extracted text",
     metadata: {
       fileName: "document.pdf",
       mimeType: "application/pdf",
       pages: 5,
       fileSize: 123456,
       parsedAt: "2024-01-15T10:30:00.000Z"
     }
   }
   ```

---

## GitHub Storage Process

### GitHub Configuration
**File:** `backend/github-config.js`

### User Repository Mapping
```javascript
const userRepos = {
  'rachitjainemail@gmail.com': {
    owner: 'Rachit19000',
    repo: 'files_storage',
    branch: 'main'
  }
};
```

### Storage Process

1. **Get Repository Info:**
   - Looks up user email in `userRepos` map
   - Gets owner, repo, branch

2. **Encode Content:**
   ```javascript
   const encodedContent = Buffer.from(content).toString('base64');
   ```
   - GitHub API requires base64 encoding

3. **Check Existing File:**
   ```javascript
   octokit.repos.getContent({ path: filePath })
   ```
   - Gets SHA if file exists (for updates)

4. **Create/Update File:**
   ```javascript
   octokit.repos.createOrUpdateFileContents({
     owner: 'Rachit19000',
     repo: 'files_storage',
     path: 'requirements/user_1/job_123/file.txt',
     message: 'Add requirement: file.txt',
     content: base64EncodedText,
     branch: 'main',
     sha: sha // if updating
   })
   ```

5. **GitHub Response:**
   ```javascript
   {
     content: {
       html_url: "https://github.com/.../blob/main/...",
       sha: "abc123..."
     },
     commit: {
       html_url: "https://github.com/.../commit/xyz789"
     }
   }
   ```

### Folder Structure in GitHub

```
files_storage/
  └── requirements/
      └── user_1/
          └── job_1703123456789/
              └── document_extracted.txt
```

---

## Complete Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        FRONTEND (React)                         │
│                    http://localhost:3000                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ 1. User Login
                              ▼
                    ┌─────────────────────┐
                    │   Login Component   │
                    │  (Login.js)         │
                    └─────────────────────┘
                              │
                              │ POST /api/v1/auth/login
                              │ { email, password }
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        BACKEND (Express)                        │
│                    http://localhost:3001                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────────────────┐
                    │  Auth Endpoint      │
                    │  (server.js)        │
                    └─────────────────────┘
                              │
                              │ Returns: { token, user }
                              ▼
                    ┌─────────────────────┐
                    │  Store in           │
                    │  localStorage       │
                    └─────────────────────┘
                              │
                              │ 2. User Uploads File
                              ▼
                    ┌─────────────────────┐
                    │ RequirementUpload   │
                    │ Component           │
                    └─────────────────────┘
                              │
                              │ POST /api/v1/requirements/upload
                              │ FormData: { file }
                              │ Headers: { Authorization: Bearer token }
                              ▼
                    ┌─────────────────────┐
                    │  Multer Middleware  │
                    │  (upload.single)   │
                    └─────────────────────┘
                              │
                              │ req.file = { buffer, originalname, mimetype }
                              ▼
                    ┌─────────────────────┐
                    │  File Upload        │
                    │  Endpoint           │
                    │  (server.js:109)    │
                    └─────────────────────┘
                              │
                              │ 3. Document Parsing
                              ▼
                    ┌─────────────────────┐
                    │  Document Parser    │
                    │  Agent              │
                    │  (document-parser)  │
                    └─────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    │                   │
            ┌───────▼───────┐   ┌───────▼───────┐
            │  PDF Parser   │   │  DOCX Parser  │
            │  (pdf-parse)  │   │  (mammoth)    │
            └───────┬───────┘   └───────┬───────┘
                    │                   │
                    └─────────┬─────────┘
                              │
                              │ Returns: { text, metadata }
                              ▼
                    ┌─────────────────────┐
                    │  Text Cleaning      │
                    │  (normalize, trim)  │
                    └─────────────────────┘
                              │
                              │ 4. GitHub Storage
                              ▼
                    ┌─────────────────────┐
                    │  GitHub Config      │
                    │  (github-config.js) │
                    └─────────────────────┘
                              │
                              │ createFileInGitHub()
                              │ - Get repo config
                              │ - Encode to base64
                              │ - Call GitHub API
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      GITHUB API (Octokit)                       │
│              https://api.github.com/repos/...                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────────────────┐
                    │  Create/Update      │
                    │  File Contents     │
                    │  API Call          │
                    └─────────────────────┘
                              │
                              │ Returns: { fileUrl, commitUrl }
                              ▼
                    ┌─────────────────────┐
                    │  Response to        │
                    │  Frontend           │
                    └─────────────────────┘
                              │
                              │ JSON Response
                              ▼
                    ┌─────────────────────┐
                    │  Display Success    │
                    │  Message + GitHub   │
                    │  URL                │
                    └─────────────────────┘
```

---

## API Endpoints Summary

### Authentication
- **POST** `/api/v1/auth/login`
  - Body: `{ email, password }`
  - Response: `{ token, user }`

### File Upload
- **POST** `/api/v1/requirements/upload`
  - Headers: `Authorization: Bearer {token}`
  - Body: `FormData { file }`
  - Response: `{ jobId, githubUrl, commitUrl, ... }`

### Text Paste
- **POST** `/api/v1/requirements/paste`
  - Headers: `Authorization: Bearer {token}`, `Content-Type: application/json`
  - Body: `{ text, name }`
  - Response: `{ jobId, githubUrl, commitUrl, ... }`

---

## Key Files and Their Roles

| File | Purpose |
|------|---------|
| `frontend/src/components/Login.js` | Login UI and API call |
| `frontend/src/components/RequirementUpload.js` | Upload UI and API calls |
| `backend/server.js` | Express server, API endpoints |
| `backend/document-parser.js` | Document parsing logic |
| `backend/github-config.js` | GitHub API integration |
| `backend/package.json` | Dependencies: pdf-parse, mammoth, @octokit/rest |

---

## Error Handling

### Frontend Errors
- Network errors → "Backend API is not running"
- API errors → Display error message from backend
- Validation errors → Show inline error messages

### Backend Errors
- Authentication errors → 401 Unauthorized
- File type errors → 400 Bad Request
- Parsing errors → 500 Internal Server Error with details
- GitHub errors → 500 with GitHub API error message

---

## Complete Example Flow

### Scenario: User uploads a PDF file

1. **User logs in** → Token stored: `mock_token_user_1_1703123456789`

2. **User selects PDF** → `document.pdf` (5 pages, 500KB)

3. **Frontend sends:**
   ```
   POST http://localhost:3001/api/v1/requirements/upload
   Authorization: Bearer mock_token_user_1_1703123456789
   Body: FormData { file: document.pdf }
   ```

4. **Backend receives:**
   - File buffer in memory
   - MIME type: `application/pdf`
   - File name: `document.pdf`

5. **Parser extracts:**
   - Text from all 5 pages
   - 1234 characters extracted
   - Metadata: { pages: 5, fileSize: 500000 }

6. **Text cleaned:**
   - Line breaks normalized
   - Blank lines reduced
   - Whitespace trimmed

7. **GitHub storage:**
   - Path: `requirements/user_1/job_1703123456789/document_extracted.txt`
   - Content: Base64 encoded extracted text
   - Commit: "Add parsed requirement: document.pdf (Job: job_123) - 5 pages - 1234 characters extracted"

8. **GitHub returns:**
   - File URL: `https://github.com/Rachit19000/files_storage/blob/main/requirements/user_1/job_123/document_extracted.txt`
   - Commit URL: `https://github.com/Rachit19000/files_storage/commit/abc123`

9. **Frontend displays:**
   ```
   File parsed and uploaded to GitHub!
   Job ID: job_1703123456789
   Extracted 1234 characters
   View file: https://github.com/...
   ```

10. **User checks GitHub:**
    - Navigates to `files_storage` repo
    - Sees `requirements/user_1/job_123/document_extracted.txt`
    - Can read the extracted text directly

---

## Summary

**Complete Flow:**
1. User → Frontend (React) → API Call
2. Backend (Express) → Authentication → File Receipt
3. Document Parser → Extract Text → Clean Text
4. GitHub API → Store Text → Return URLs
5. Backend → Response → Frontend → Display Success

**Key Technologies:**
- **Frontend:** React, Fetch API, localStorage
- **Backend:** Express, Multer, pdf-parse, mammoth
- **Storage:** GitHub API (Octokit)
- **Authentication:** Mock tokens (localStorage)

**Data Transformation:**
- Binary File (PDF/DOCX) → Text Extraction → Cleaned Text → Base64 → GitHub Storage
