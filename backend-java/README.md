# SDLC Automation Backend (Java/Spring Boot)

## Overview
This is the Java/Spring Boot backend for the SDLC Automation Platform. It provides RESTful APIs for authentication, requirement upload, document parsing, and GitHub integration.

---

## Technology Stack

- **Java**: 17
- **Spring Boot**: 3.2.1
- **Build Tool**: Maven
- **Document Parsing**:
  - Apache PDFBox (PDF parsing)
  - Apache POI (Word document parsing)
- **GitHub Integration**: GitHub API Java (`org.kohsuke:github-api`)

---

## Project Structure

```
backend-java/
├── src/main/java/com/sdlc/
│   ├── SdlcBackendApplication.java         # Main application
│   ├── config/
│   │   └── CorsConfig.java                  # CORS configuration
│   ├── controller/
│   │   ├── AuthController.java              # Authentication endpoints
│   │   └── RequirementsController.java      # Upload endpoints
│   ├── service/
│   │   ├── AuthService.java                 # Authentication logic
│   │   ├── DocumentParserService.java       # Document parsing
│   │   └── GitHubService.java               # GitHub integration
│   ├── repository/
│   │   └── UserRepository.java              # User data (mock)
│   ├── model/
│   │   └── User.java                        # User entity
│   ├── dto/
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   ├── TextUploadRequest.java
│   │   ├── UploadResponse.java
│   │   └── ErrorResponse.java
│   └── exception/
│       └── GlobalExceptionHandler.java      # Global error handling
├── src/main/resources/
│   └── application.properties               # Configuration
└── pom.xml                                  # Maven dependencies
```

---

## Prerequisites

- **Java 17** or higher
- **Maven 3.6+**
- **GitHub Personal Access Token** with `repo` scope

---

## Setup Instructions

### Step 1: Install Java

Check if Java is installed:
```bash
java -version
```

If not installed, download from: https://adoptium.net/

### Step 2: Install Maven

Check if Maven is installed:
```bash
mvn -version
```

If not installed, download from: https://maven.apache.org/download.cgi

### Step 3: Configure GitHub Token

Create `.env` file in `backend-java/` folder:
```env
GITHUB_TOKEN=your_github_token_here
```

Or set as environment variable:
```bash
# Windows PowerShell
$env:GITHUB_TOKEN="your_github_token_here"

# Linux/Mac
export GITHUB_TOKEN=your_github_token_here
```

### Step 4: Build the Project

```bash
cd backend-java
mvn clean install
```

### Step 5: Run the Application

```bash
mvn spring-boot:run
```

Or run the JAR:
```bash
java -jar target/sdlc-automation-backend-1.0.0.jar
```

The server will start on: `http://localhost:3001/api/v1`

---

## API Endpoints

### Authentication

#### POST `/api/v1/auth/login`
Login with email and password

**Request:**
```json
{
  "email": "rachitjainemail@gmail.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "token": "mock_token_user_1_1703123456789",
  "user": {
    "id": "user_1",
    "email": "rachitjainemail@gmail.com",
    "name": "Rachit Jain"
  }
}
```

#### GET `/api/v1/auth/health`
Health check endpoint

---

### Requirements Upload

#### POST `/api/v1/requirements/upload`
Upload a file (PDF/DOCX/TXT) - extracts text and stores in GitHub

**Headers:**
```
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Form Data:**
- `file`: The file to upload
- `name`: (optional) File name

**Response:**
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
  "metadata": {
    "pages": 5,
    "fileName": "document.pdf",
    "mimeType": "application/pdf"
  }
}
```

#### POST `/api/v1/requirements/paste`
Upload pasted text - stores in GitHub

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Request:**
```json
{
  "text": "Your requirement text here",
  "name": "Pasted Requirements"
}
```

**Response:**
```json
{
  "jobId": "job_1703123456789",
  "status": "stored",
  "message": "Text uploaded successfully to GitHub",
  "githubUrl": "https://github.com/.../requirement.txt",
  "commitUrl": "https://github.com/.../commit/xyz789",
  "filePath": "requirements/user_1/job_123/requirement.txt",
  "fileName": "requirement_job_123.txt"
}
```

---

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# Server port
server.port=3001

# GitHub token (or use environment variable)
github.token=${GITHUB_TOKEN:}

# User repository mapping
github.user.rachitjainemail@gmail.com.owner=Rachit19000
github.user.rachitjainemail@gmail.com.repo=files_storage
github.user.rachitjainemail@gmail.com.branch=main

# File upload limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

---

## Test Credentials

- Email: `rachitjainemail@gmail.com`
- Password: `password123`

Additional test users:
- `test@example.com` / `test123`
- `admin@example.com` / `admin123`

---

## Document Parsing

### Supported Formats

| Format | Library | Status |
|--------|---------|--------|
| PDF | Apache PDFBox | ✅ |
| DOCX | Apache POI | ✅ |
| TXT | Native Java | ✅ |

### Process

1. File uploaded → Multer receives
2. Document Parser extracts text
3. Text cleaned and normalized
4. Stored in GitHub as `.txt` file

---

## Troubleshooting

### "GITHUB_TOKEN is required"
- Set the environment variable: `GITHUB_TOKEN=your_token_here`
- Or create `.env` file (see Step 3)

### "Port 3001 is already in use"
- Stop the Node.js backend if running
- Or change port in `application.properties`

### Maven build fails
- Ensure Java 17 is installed
- Run: `mvn clean install -U`

### GitHub authentication fails
- Verify token has `repo` scope
- Check token is not expired
- Ensure token is set correctly

---

## Running with Node.js Backend Side-by-Side

If you want to keep both running:

1. Change Java backend port in `application.properties`:
   ```properties
   server.port=8080
   ```

2. Update frontend API URL to match the backend you want to use

---

## Development

### Hot Reload
Spring Boot DevTools is included for automatic restart on code changes.

### Build JAR
```bash
mvn clean package
```

Output: `target/sdlc-automation-backend-1.0.0.jar`

### Run JAR
```bash
java -jar target/sdlc-automation-backend-1.0.0.jar
```

---

## Next Steps

1. Replace mock users with database (PostgreSQL)
2. Implement JWT authentication
3. Add WebSocket support for real-time updates
4. Implement AI agent orchestration
5. Add Redis for job queues

See `docs/` folder for complete architecture documentation.
