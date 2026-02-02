# AI-Driven SDLC Automation Platform - Java/Spring Boot Architecture

## Technology Stack Update

### Frontend (No Change)
- **Framework**: React 18
- **Routing**: React Router DOM v6
- **Styling**: CSS3
- **API Client**: Fetch API
- **Port**: 3000

### Backend (New - Java/Spring Boot)
- **Language**: Java 17
- **Framework**: Spring Boot 3.2.1
- **Build Tool**: Maven
- **Document Parsing**:
  - Apache PDFBox 3.0.1 (PDF extraction)
  - Apache POI 5.2.5 (Word document extraction)
- **GitHub Integration**: GitHub API Java 1.318
- **Port**: 3001
- **Context Path**: `/api/v1`

---

## Architecture Layers

```
┌──────────────────────────────────────────────────────────────┐
│                     FRONTEND LAYER                           │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  React Application (Port 3000)                         │  │
│  │  - Login Component                                      │  │
│  │  - Dashboard Page                                       │  │
│  │  - RequirementUpload Component                         │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
                          ↕ HTTP/REST
┌──────────────────────────────────────────────────────────────┐
│                  SPRING BOOT BACKEND                         │
│                   (Port 3001)                                │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  Controllers (@RestController)                         │  │
│  │  - AuthController (/auth/*)                            │  │
│  │  - RequirementsController (/requirements/*)           │  │
│  └────────────────────────────────────────────────────────┘  │
│                          ↕                                   │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  Services (@Service)                                   │  │
│  │  - AuthService (Login, token validation)              │  │
│  │  - DocumentParserService (PDF/DOCX/TXT parsing)       │  │
│  │  - GitHubService (File storage in GitHub)             │  │
│  └────────────────────────────────────────────────────────┘  │
│                          ↕                                   │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  Repositories (@Repository)                            │  │
│  │  - UserRepository (Mock user data)                     │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
                          ↕ GitHub API
┌──────────────────────────────────────────────────────────────┐
│                   EXTERNAL SERVICES                          │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  GitHub API (github-api.kohsuke.org)                   │  │
│  │  - File creation/update                                 │  │
│  │  - Commit management                                    │  │
│  │  - Repository access                                    │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
                          ↕
┌──────────────────────────────────────────────────────────────┐
│                   STORAGE LAYER                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  GitHub Repository (Rachit19000/files_storage)         │  │
│  │  - requirements/{userId}/{jobId}/file_extracted.txt    │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

---

## Component Details

### 1. Controllers (REST Layer)

#### AuthController
- **Path**: `/api/v1/auth`
- **Endpoints**:
  - `POST /login` - User authentication
  - `GET /health` - Health check
- **Responsibilities**:
  - Receive login requests
  - Validate input using Bean Validation
  - Delegate to AuthService
  - Return LoginResponse or ErrorResponse

#### RequirementsController
- **Path**: `/api/v1/requirements`
- **Endpoints**:
  - `POST /upload` - File upload (PDF/DOCX/TXT)
  - `POST /paste` - Text paste
- **Responsibilities**:
  - Authenticate user via token
  - Receive multipart files or JSON text
  - Delegate to services
  - Return UploadResponse with GitHub URLs

---

### 2. Services (Business Logic Layer)

#### AuthService
- **Purpose**: User authentication and token management
- **Methods**:
  - `login(LoginRequest)` - Authenticate user
  - `validateToken(String)` - Validate and extract user from token
- **Logic**:
  - Check email/password against UserRepository
  - Generate mock token
  - Return user details

#### DocumentParserService
- **Purpose**: Extract text from documents
- **Methods**:
  - `parseDocument(MultipartFile)` - Main parsing method
  - `parsePdf(InputStream)` - PDF parsing using Apache PDFBox
  - `parseDocx(InputStream)` - DOCX parsing using Apache POI
  - `parseTxt(InputStream)` - Plain text parsing
  - `cleanText(String)` - Text normalization
  - `isSupportedFileType(String, String)` - File type validation
- **Libraries**:
  - Apache PDFBox: PDF text extraction
  - Apache POI: Word document extraction
- **Output**: ParseResult { text, metadata }

#### GitHubService
- **Purpose**: GitHub integration for file storage
- **Methods**:
  - `createFileInGitHub(email, path, content, message)` - Create/update file
  - `getUserRepo(email)` - Get repo config for user
- **Library**: GitHub API Java (org.kohsuke:github-api)
- **Process**:
  1. Get repository from user email mapping
  2. Check if file exists (get SHA)
  3. Create or update file via GitHub API
  4. Return file URL and commit URL

---

### 3. Data Models

#### User
```java
class User {
  String id;
  String email;
  String password;
  String name;
}
```

#### LoginRequest/Response
```java
class LoginRequest {
  @Email String email;
  @NotBlank String password;
}

class LoginResponse {
  String token;
  UserDto user;
}
```

#### UploadResponse
```java
class UploadResponse {
  String jobId;
  String status;
  String githubUrl;
  String commitUrl;
  String filePath;
  String fileName;
  Integer extractedTextLength;
  Map<String, Object> metadata;
}
```

---

## Request Flow

### File Upload Flow

```
1. User uploads PDF file
   ↓
2. Frontend: RequirementUpload.js
   - Creates FormData
   - Adds Authorization header
   - POST /api/v1/requirements/upload
   ↓
3. Backend: RequirementsController
   - @PostMapping("/upload")
   - Receives MultipartFile
   - Validates token
   ↓
4. DocumentParserService
   - Detects file type (PDF)
   - Calls parsePdf()
   - Uses Apache PDFBox
   - Extracts text from all pages
   - Returns ParseResult
   ↓
5. GitHubService
   - Gets repo config for user
   - Creates file path: requirements/user_1/job_123/file_extracted.txt
   - Calls GitHub API (org.kohsuke.github)
   - Creates file in repository
   - Returns file URL and commit URL
   ↓
6. RequirementsController
   - Builds UploadResponse
   - Returns JSON response
   ↓
7. Frontend: RequirementUpload.js
   - Displays success message
   - Shows GitHub URL
```

---

## API Endpoints Mapping

| Endpoint | Node.js | Spring Boot | Method |
|----------|---------|-------------|--------|
| Login | `/api/v1/auth/login` | `/api/v1/auth/login` | POST |
| Health | `/api/v1/health` | `/api/v1/auth/health` | GET |
| Upload File | `/api/v1/requirements/upload` | `/api/v1/requirements/upload` | POST |
| Paste Text | `/api/v1/requirements/paste` | `/api/v1/requirements/paste` | POST |

**Note:** Frontend does not need changes - endpoints are identical

---

## Library Mapping

| Feature | Node.js | Spring Boot |
|---------|---------|-------------|
| Web Framework | Express.js | Spring Boot Web MVC |
| File Upload | Multer | Spring Multipart |
| PDF Parsing | pdf-parse | Apache PDFBox |
| Word Parsing | mammoth | Apache POI |
| GitHub API | @octokit/rest | github-api (kohsuke) |
| CORS | cors package | Spring CorsFilter |
| Logging | console.log | SLF4J + Logback |

---

## Configuration

### application.properties
```properties
# Server
server.port=3001
server.servlet.context-path=/api/v1

# CORS
spring.web.cors.allowed-origins=http://localhost:3000

# File Upload
spring.servlet.multipart.max-file-size=10MB

# GitHub
github.token=${GITHUB_TOKEN:}
github.user.rachitjainemail@gmail.com.owner=Rachit19000
github.user.rachitjainemail@gmail.com.repo=files_storage
github.user.rachitjainemail@gmail.com.branch=main
```

---

## Advantages of Java/Spring Boot

1. **Type Safety**: Compile-time error checking
2. **Better IDE Support**: IntelliJ, Eclipse
3. **Enterprise Ready**: Production-grade features
4. **Performance**: Better for CPU-intensive tasks (document parsing)
5. **Dependency Injection**: Built-in with Spring
6. **Exception Handling**: Global @RestControllerAdvice
7. **Validation**: Bean Validation (javax.validation)
8. **Documentation**: Swagger/OpenAPI integration easier

---

## Migration Notes

### What Changed
- Node.js → Java 17
- Express.js → Spring Boot
- npm packages → Maven dependencies
- JavaScript → Java (strongly typed)

### What Stayed Same
- API endpoints (same URLs)
- Request/Response formats (same JSON)
- Frontend code (no changes needed)
- GitHub integration (same API)
- Folder structure in GitHub repo

### No Changes Needed In
- Frontend React application
- API contracts
- GitHub repository structure
- Environment variables (just set GITHUB_TOKEN)

---

## Running the Java Backend

```bash
# Build
cd backend-java
mvn clean install

# Run
mvn spring-boot:run

# Or run JAR
java -jar target/sdlc-automation-backend-1.0.0.jar
```

**Output:**
```
🚀 SDLC Backend running on http://localhost:3001/api/v1
📝 Test login with: rachitjainemail@gmail.com / password123
```

---

## Next Steps for Full Implementation

1. **Database Integration**
   - Replace UserRepository mock with JPA
   - Add entities for workflows, jobs, artifacts

2. **JWT Authentication**
   - Use Spring Security + JWT
   - Token signing and validation

3. **AI Agent Integration**
   - Create agent services
   - Integrate OpenAI/Claude APIs

4. **WebSocket Support**
   - Add Spring WebSocket
   - Real-time progress updates

5. **Redis Queue**
   - Add Spring Data Redis
   - Job queue for async processing

6. **Monitoring**
   - Spring Actuator
   - Prometheus metrics
   - Health checks

See `backend-java/README.md` for detailed setup instructions.
