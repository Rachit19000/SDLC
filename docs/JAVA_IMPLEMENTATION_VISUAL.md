# Java/Spring Boot Implementation - Visual Guide

## 🏗️ Complete Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                         FRONTEND (React)                             │
│                      http://localhost:3000                           │
│  ┌───────────────┐  ┌───────────────┐  ┌─────────────────────────┐ │
│  │  Login Page   │  │  Dashboard    │  │  RequirementUpload      │ │
│  │  (Login.js)   │→ │  (Dashboard.js)│→ │  (RequirementUpload.js) │ │
│  └───────────────┘  └───────────────┘  └─────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
                              ↓ HTTP/REST
┌─────────────────────────────────────────────────────────────────────┐
│                  JAVA/SPRING BOOT BACKEND                            │
│                      http://localhost:3001/api/v1                    │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  CONTROLLERS (@RestController)                               │   │
│  │  ┌──────────────────┐  ┌─────────────────────────────────┐ │   │
│  │  │ AuthController   │  │ RequirementsController          │ │   │
│  │  │ /auth/login      │  │ /requirements/upload            │ │   │
│  │  │ /auth/health     │  │ /requirements/paste             │ │   │
│  │  └──────────────────┘  └─────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                              ↓                                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  SERVICES (@Service)                                         │   │
│  │  ┌─────────────┐ ┌────────────────────┐ ┌───────────────┐ │   │
│  │  │ AuthService │ │DocumentParserService│ │ GitHubService │ │   │
│  │  │             │ │                     │ │               │ │   │
│  │  │ • login()   │ │ • parseDocument()   │ │ • create      │ │   │
│  │  │ • validate  │ │ • parsePdf()        │ │   FileInGitHub│ │   │
│  │  │   Token()   │ │ • parseDocx()       │ │ • getUserRepo│ │   │
│  │  │             │ │ • parseTxt()        │ │               │ │   │
│  │  └─────────────┘ └────────────────────┘ └───────────────┘ │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                              ↓                                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  REPOSITORIES (@Repository)                                  │   │
│  │  ┌──────────────────────────────────────────────────────┐  │   │
│  │  │ UserRepository (Mock In-Memory)                       │  │   │
│  │  │ • findByEmailAndPassword()                            │  │   │
│  │  │ • findById()                                          │  │   │
│  │  └──────────────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  LIBRARIES                                                   │   │
│  │  • Apache PDFBox (PDF parsing)                              │   │
│  │  • Apache POI (Word parsing)                                │   │
│  │  • GitHub API Java (GitHub integration)                     │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                              ↓ GitHub API
┌─────────────────────────────────────────────────────────────────────┐
│                     GITHUB REPOSITORY                                │
│              Rachit19000/files_storage                               │
│                                                                       │
│  requirements/                                                       │
│  └── user_1/                                                         │
│      └── job_1738234567890/                                         │
│          ├── document_extracted.txt                                 │
│          └── pasted_text.txt                                        │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 📊 Request Flow Diagrams

### 1. Login Flow

```
User enters email/password
         ↓
Frontend: Login.js
  POST /api/v1/auth/login
  Body: { email, password }
         ↓
Backend: AuthController
  @PostMapping("/login")
         ↓
AuthService
  • Check credentials
  • Generate token
         ↓
UserRepository
  • Find user by email/password
         ↓
Response: { token, user }
         ↓
Frontend: Store token in localStorage
         ↓
Redirect to Dashboard
```

---

### 2. File Upload Flow

```
User selects PDF file
         ↓
Frontend: RequirementUpload.js
  POST /api/v1/requirements/upload
  FormData with file
  Authorization: Bearer {token}
         ↓
Backend: RequirementsController
  @PostMapping("/upload")
  • Validate token
  • Receive MultipartFile
         ↓
DocumentParserService
  • Detect file type (PDF)
  • Extract text with Apache PDFBox
  • Clean and normalize text
         ↓
GitHubService
  • Create file path
  • Encode content to base64
  • Call GitHub API
  • Create file in repository
         ↓
Response: { 
  jobId,
  githubUrl,
  extractedTextLength,
  metadata
}
         ↓
Frontend: Display success message
         ↓
User views file in GitHub
```

---

### 3. Text Paste Flow

```
User pastes text
         ↓
Frontend: RequirementUpload.js
  POST /api/v1/requirements/paste
  Body: { text, name }
  Authorization: Bearer {token}
         ↓
Backend: RequirementsController
  @PostMapping("/paste")
  • Validate token
  • Receive text
         ↓
GitHubService
  • Create file path
  • Call GitHub API
  • Store text in repository
         ↓
Response: { 
  jobId,
  githubUrl,
  filePath
}
         ↓
Frontend: Display success message
```

---

## 🗂️ File Organization

```
backend-java/
│
├── src/main/
│   ├── java/com/sdlc/
│   │   │
│   │   ├── SdlcBackendApplication.java  ← Entry point
│   │   │
│   │   ├── config/
│   │   │   └── CorsConfig.java          ← CORS setup
│   │   │
│   │   ├── controller/                  ← REST endpoints
│   │   │   ├── AuthController.java
│   │   │   └── RequirementsController.java
│   │   │
│   │   ├── service/                     ← Business logic
│   │   │   ├── AuthService.java
│   │   │   ├── DocumentParserService.java
│   │   │   └── GitHubService.java
│   │   │
│   │   ├── repository/                  ← Data access
│   │   │   └── UserRepository.java
│   │   │
│   │   ├── model/                       ← Entities
│   │   │   └── User.java
│   │   │
│   │   ├── dto/                         ← Request/Response
│   │   │   ├── LoginRequest.java
│   │   │   ├── LoginResponse.java
│   │   │   ├── TextUploadRequest.java
│   │   │   ├── UploadResponse.java
│   │   │   └── ErrorResponse.java
│   │   │
│   │   └── exception/                   ← Error handling
│   │       └── GlobalExceptionHandler.java
│   │
│   └── resources/
│       └── application.properties       ← Configuration
│
├── pom.xml                              ← Maven dependencies
├── .gitignore                           ← Git ignore
├── mvnw.cmd                             ← Maven wrapper
│
└── Documentation/
    ├── README.md                        ← Complete guide
    ├── QUICKSTART.md                    ← Quick setup
    └── INSTALLATION.md                  ← Installation steps
```

---

## 🔄 Data Flow

### Document Upload → GitHub Storage

```
┌──────────────┐
│  PDF File    │
│  (Binary)    │
└──────┬───────┘
       │
       ↓ MultipartFile
┌──────────────────────────────────┐
│  DocumentParserService           │
│  ┌────────────────────────────┐ │
│  │ Apache PDFBox              │ │
│  │ • Load PDF                 │ │
│  │ • Extract text from pages  │ │
│  │ • Get metadata (pages)     │ │
│  └────────────────────────────┘ │
└──────┬───────────────────────────┘
       │
       ↓ Extracted Text (String)
┌──────────────────────────────────┐
│  Text Cleaning                   │
│  • Normalize line breaks         │
│  • Remove extra whitespace       │
│  • Trim                          │
└──────┬───────────────────────────┘
       │
       ↓ Clean Text
┌──────────────────────────────────┐
│  GitHubService                   │
│  ┌────────────────────────────┐ │
│  │ GitHub API                 │ │
│  │ • Create file path         │ │
│  │ • Encode to base64         │ │
│  │ • Create/update file       │ │
│  │ • Commit to repository     │ │
│  └────────────────────────────┘ │
└──────┬───────────────────────────┘
       │
       ↓ GitHub URLs
┌──────────────────────────────────┐
│  Response                        │
│  • File URL                      │
│  • Commit URL                    │
│  • Job ID                        │
│  • Metadata                      │
└──────────────────────────────────┘
```

---

## 🎯 Component Interactions

### Dependency Graph

```
           SdlcBackendApplication
                    │
        ┌───────────┴───────────┐
        │                       │
   Controllers              CorsConfig
        │
    ┌───┴───┐
    │       │
Auth    Requirements
Controller  Controller
    │           │
    ↓           ↓
    │     ┌─────┴─────┐
    │     │           │
AuthService  DocumentParser  GitHubService
    │         Service
    ↓
UserRepository
```

---

## 🚦 Startup Sequence

```
1. JVM starts
   ↓
2. Spring Boot Application.run()
   ↓
3. Component Scanning
   ↓
4. Bean Creation
   • CorsConfig bean
   • Controllers beans
   • Services beans
   • Repository bean
   ↓
5. Dependency Injection
   • Inject services into controllers
   • Inject repository into AuthService
   ↓
6. @PostConstruct methods
   • GitHubService.initialize()
     - Connect to GitHub API
     - Validate GITHUB_TOKEN
   ↓
7. Embedded Tomcat starts
   ↓
8. Application Ready
   🚀 Backend running on port 3001
```

---

## 📦 Maven Build Process

```
mvn clean install
         ↓
1. Clean target/ directory
         ↓
2. Download dependencies
   • Spring Boot jars
   • Apache PDFBox
   • Apache POI
   • GitHub API
   • Lombok
   (~50 JARs, ~50 MB)
         ↓
3. Compile Java source
   • 15 .java files
   • Generate Lombok code
   • Compile to .class files
         ↓
4. Run tests (if any)
         ↓
5. Package JAR
   • Create executable JAR
   • Include dependencies
   • target/sdlc-automation-backend-1.0.0.jar
         ↓
BUILD SUCCESS
```

---

## 🔐 Authentication Flow

```
Login Request
      ↓
AuthController.login()
      ↓
AuthService.login(request)
      ↓
UserRepository.findByEmailAndPassword()
      ↓
Mock Users List
  • rachitjainemail@gmail.com
  • test@example.com
  • admin@example.com
      ↓
User Found? 
  YES ↓               NO → 401 Unauthorized
Generate Token
"mock_token_user_1_1738234567890"
      ↓
Return LoginResponse
  • token
  • user { id, email, name }
      ↓
Frontend stores token in localStorage
      ↓
Subsequent requests include:
Authorization: Bearer {token}
      ↓
AuthService.validateToken(token)
  • Extract user ID from token
  • Find user by ID
      ↓
Request authenticated ✅
```

---

## 📝 API Request/Response Examples

### POST /auth/login

**Request:**
```json
POST http://localhost:3001/api/v1/auth/login
Content-Type: application/json

{
  "email": "rachitjainemail@gmail.com",
  "password": "password123"
}
```

**Response:**
```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "token": "mock_token_user_1_1738234567890",
  "user": {
    "id": "user_1",
    "email": "rachitjainemail@gmail.com",
    "name": "Rachit Jain"
  }
}
```

---

### POST /requirements/upload

**Request:**
```http
POST http://localhost:3001/api/v1/requirements/upload
Authorization: Bearer mock_token_user_1_1738234567890
Content-Type: multipart/form-data

file: document.pdf (binary)
name: Project Requirements
```

**Response:**
```json
HTTP/1.1 202 Accepted
Content-Type: application/json

{
  "jobId": "job_1738234567890",
  "status": "stored",
  "message": "File parsed and text uploaded successfully to GitHub",
  "githubUrl": "https://github.com/Rachit19000/files_storage/blob/main/requirements/user_1/job_1738234567890/document_extracted.txt",
  "commitUrl": "https://github.com/Rachit19000/files_storage/commit/abc123",
  "filePath": "requirements/user_1/job_1738234567890/document_extracted.txt",
  "fileName": "document_extracted.txt",
  "originalFileName": "document.pdf",
  "extractedTextLength": 5234,
  "metadata": {
    "pages": 12,
    "fileName": "document.pdf",
    "mimeType": "application/pdf",
    "fileSize": 524288,
    "parsedAt": "2024-01-30T10:30:00Z"
  }
}
```

---

## 🎨 Technology Stack Visualization

```
┌─────────────────────────────────────────────┐
│           PRESENTATION LAYER                │
│  ┌────────────────────────────────────────┐ │
│  │  React 18 + React Router v6           │ │
│  │  Fetch API + CSS3                      │ │
│  └────────────────────────────────────────┘ │
└─────────────────────────────────────────────┘
                    ↓ HTTP/REST
┌─────────────────────────────────────────────┐
│           WEB FRAMEWORK LAYER               │
│  ┌────────────────────────────────────────┐ │
│  │  Spring Boot 3.2.1                     │ │
│  │  Spring Web MVC                        │ │
│  │  Spring Validation                     │ │
│  └────────────────────────────────────────┘ │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│           DOCUMENT PARSING LAYER            │
│  ┌─────────────┐  ┌────────────────────┐  │ │
│  │ Apache      │  │ Apache POI 5.2.5   │  │ │
│  │ PDFBox 3.0.1│  │ (Word documents)   │  │ │
│  │ (PDF files) │  │                    │  │ │
│  └─────────────┘  └────────────────────┘  │ │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│           EXTERNAL API LAYER                │
│  ┌────────────────────────────────────────┐ │
│  │  GitHub API Java 1.318                 │ │
│  │  (org.kohsuke:github-api)              │ │
│  └────────────────────────────────────────┘ │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│           STORAGE LAYER                     │
│  ┌────────────────────────────────────────┐ │
│  │  GitHub Repository                     │ │
│  │  Rachit19000/files_storage             │ │
│  └────────────────────────────────────────┘ │
└─────────────────────────────────────────────┘
```

---

## ✅ Implementation Completeness

```
COMPLETED ✅
├── Core Application
│   ├── Main application class ✅
│   ├── Spring Boot configuration ✅
│   └── CORS configuration ✅
│
├── Controllers (REST Layer)
│   ├── AuthController ✅
│   └── RequirementsController ✅
│
├── Services (Business Logic)
│   ├── AuthService ✅
│   ├── DocumentParserService ✅
│   └── GitHubService ✅
│
├── Repositories (Data Layer)
│   └── UserRepository ✅
│
├── Models & DTOs
│   ├── User ✅
│   ├── LoginRequest/Response ✅
│   ├── TextUploadRequest ✅
│   ├── UploadResponse ✅
│   └── ErrorResponse ✅
│
├── Exception Handling
│   └── GlobalExceptionHandler ✅
│
├── Dependencies (Maven)
│   ├── Spring Boot ✅
│   ├── Apache PDFBox ✅
│   ├── Apache POI ✅
│   └── GitHub API ✅
│
└── Documentation
    ├── README ✅
    ├── QUICKSTART ✅
    ├── INSTALLATION ✅
    ├── Architecture docs ✅
    └── Comparison docs ✅

STATUS: 100% COMPLETE ✅
```

---

**🎉 Visual guide complete! See `COMPLETE_IMPLEMENTATION_SUMMARY.md` for next steps.**
