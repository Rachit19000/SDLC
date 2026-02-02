# AI-Driven SDLC Automation Platform

> Transform raw requirements into complete, AI-generated SDLC artifacts with human validation at every step.

---

## Overview

This platform automates the entire Software Development Life Cycle by leveraging AI agents to generate:
- Work Breakdown Structure (WBS)
- User Stories & Epics
- Technical Specifications
- Non-Functional Requirements (NFRs)
- Deployment Architecture
- Sprint/Release Plans
- Functional Test Scenarios
- Performance Testing Approaches
- Workspace Structures for Development

---

## Technology Stack

### Frontend
- **React 18** - UI framework
- **React Router v6** - Navigation
- **CSS3** - Styling
- **Fetch API** - HTTP client

### Backend (Choose One)

#### Option 1: Node.js (Original)
- **Node.js** + Express.js
- **Document Parsing**: pdf-parse, mammoth
- **GitHub Integration**: @octokit/rest

#### Option 2: Java/Spring Boot (New) ⭐
- **Java 17** + Spring Boot 3.2.1
- **Document Parsing**: Apache PDFBox, Apache POI
- **GitHub Integration**: GitHub API Java
- **Build Tool**: Maven

### External Services
- **GitHub** - Version control and file storage
- **OpenAI/Claude** - AI processing (planned)

---

## Project Structure

```
SDLC/
├── frontend/              # React application
│   ├── src/
│   │   ├── components/    # Login, RequirementUpload
│   │   ├── pages/         # Dashboard
│   │   └── App.js         # Main app with routing
│   └── package.json
│
├── backend/               # Node.js backend (original)
│   ├── server.js
│   ├── document-parser.js
│   ├── github-config.js
│   └── package.json
│
├── backend-java/          # Java/Spring Boot backend (NEW)
│   ├── src/main/java/com/sdlc/
│   │   ├── controller/    # REST endpoints
│   │   ├── service/       # Business logic
│   │   ├── repository/    # Data access
│   │   ├── model/         # Entities
│   │   ├── dto/           # Data transfer objects
│   │   └── exception/     # Error handling
│   ├── src/main/resources/
│   │   └── application.properties
│   ├── pom.xml
│   ├── README.md
│   ├── INSTALLATION.md
│   └── QUICKSTART.md
│
└── docs/                  # Architecture documentation
    ├── JAVA_ARCHITECTURE.md
    ├── NODE_VS_JAVA_COMPARISON.md
    ├── COMPLETE_WORKFLOW.md
    ├── ARCHITECTURE_EXPLAINED.md
    ├── VISUAL_FLOW_DIAGRAM.md
    └── ...
```

---

## Quick Start

### Prerequisites
- **Node.js 18+** (for frontend + Node.js backend)
- **Java 17+** (for Java backend)
- **Maven 3.6+** (for Java backend)
- **GitHub Personal Access Token** with `repo` scope

---

### Option A: Run with Java Backend (Recommended)

#### 1. Start Java Backend
```bash
# Set GitHub token
$env:GITHUB_TOKEN="your_github_token_here"

# Build and run
cd backend-java
mvn clean install
mvn spring-boot:run
```

**Expected output:**
```
🚀 SDLC Backend running on http://localhost:3001/api/v1
```

#### 2. Start Frontend
```bash
cd frontend
npm install
npm start
```

**Opens:** http://localhost:3000

#### 3. Login & Upload
- **Email:** `rachitjainemail@gmail.com`
- **Password:** `password123`
- Upload PDF/DOCX/TXT files
- Extracted text stored in GitHub

---

### Option B: Run with Node.js Backend

#### 1. Start Node.js Backend
```bash
# Set GitHub token
$env:GITHUB_TOKEN="your_github_token_here"

# Install and run
cd backend
npm install
npm start
```

#### 2. Start Frontend
```bash
cd frontend
npm install
npm start
```

---

## Features Implemented

### ✅ Current Features
- [x] User authentication (mock)
- [x] Document upload (PDF, DOCX, TXT)
- [x] Text extraction from documents
- [x] Text paste interface
- [x] GitHub storage integration
- [x] Real-time error handling
- [x] CORS configuration
- [x] File size validation (10MB)

### 🚧 In Progress
- [ ] Database integration (PostgreSQL)
- [ ] JWT authentication
- [ ] AI agent orchestration
- [ ] WebSocket for real-time updates
- [ ] Redis job queue

### 📋 Planned Features
- [ ] WBS generation
- [ ] User stories generation
- [ ] Technical specs generation
- [ ] NFR generation
- [ ] Deployment architecture
- [ ] Sprint planning
- [ ] Test case generation

---

## API Endpoints

### Authentication
- `POST /api/v1/auth/login` - User login
- `GET /api/v1/auth/health` - Health check

### Requirements
- `POST /api/v1/requirements/upload` - Upload file (PDF/DOCX/TXT)
- `POST /api/v1/requirements/paste` - Paste text

---

## Documentation

### Setup Guides
- **Java Backend**: `backend-java/README.md`, `backend-java/INSTALLATION.md`, `backend-java/QUICKSTART.md`
- **Node.js Backend**: `backend/README.md`, `backend/GITHUB_SETUP.md`
- **Migration**: `MIGRATION_GUIDE.md`

### Architecture
- **Java Architecture**: `docs/JAVA_ARCHITECTURE.md`
- **Node vs Java**: `docs/NODE_VS_JAVA_COMPARISON.md`
- **Complete Workflow**: `docs/COMPLETE_WORKFLOW.md`
- **Architecture Explained**: `docs/ARCHITECTURE_EXPLAINED.md`
- **Visual Flow**: `docs/VISUAL_FLOW_DIAGRAM.md`

---

## Backend Comparison

| Feature | Node.js | Java/Spring Boot |
|---------|---------|------------------|
| **Startup** | Fast (~1s) | Medium (~3-5s) |
| **Memory** | Low (50-100MB) | Medium (200-300MB) |
| **Type Safety** | ❌ JavaScript | ✅ Java |
| **PDF Parsing** | pdf-parse | Apache PDFBox |
| **Word Parsing** | mammoth | Apache POI |
| **Concurrency** | Event loop | Thread pool |
| **Enterprise** | Good | Excellent |

**Recommendation:** Java/Spring Boot for production, Node.js for rapid prototyping.

---

## Workflow

1. **Upload Requirements**
   - User uploads PDF/DOCX or pastes text
   - Frontend sends to backend

2. **Parse Document**
   - Backend extracts clean text
   - Normalizes formatting

3. **Store in GitHub**
   - Extracted text committed to repo
   - `requirements/{userId}/{jobId}/file.txt`

4. **Future: AI Processing**
   - Text sent to AI agents
   - Generate SDLC artifacts
   - Human validation at each step

---

## Environment Variables

```bash
# GitHub Personal Access Token (required)
GITHUB_TOKEN=your_github_token_here
```

Get token from: https://github.com/settings/tokens
Required scopes: `repo`

---

## GitHub Storage Structure

```
Rachit19000/files_storage (repository)
└── requirements/
    └── user_1/
        └── job_1738234567890/
            ├── document_extracted.txt
            └── requirement_text.txt
```

---

## Testing

### Test Credentials
- Email: `rachitjainemail@gmail.com`
- Password: `password123`

Additional test users:
- `test@example.com` / `test123`
- `admin@example.com` / `admin123`

### Test Files
Upload any:
- PDF document
- Word document (.docx)
- Text file (.txt)

Verify in GitHub: https://github.com/Rachit19000/files_storage

---

## Troubleshooting

### "Backend API is not running"
- Check backend is started
- Check port 3001 is not blocked
- Visit: http://localhost:3001/api/v1/auth/health

### "GITHUB_TOKEN is required" (Java)
```bash
$env:GITHUB_TOKEN="your_token_here"
mvn spring-boot:run
```

### "Port 3001 already in use"
- Stop other backend
- Or change port in config

### "Cannot parse document"
- Check file type (PDF/DOCX/TXT only)
- Check file size (max 10MB)
- Check file is not corrupted

---

## Development

### Frontend Development
```bash
cd frontend
npm start
# Runs on http://localhost:3000
```

### Java Backend Development
```bash
cd backend-java
mvn spring-boot:run
# Runs on http://localhost:3001/api/v1
```

### Node.js Backend Development
```bash
cd backend
npm start
# Runs on http://localhost:3001/api/v1
```

---

## Contributing

1. Choose backend: Java or Node.js
2. Follow architecture in `docs/`
3. Test with frontend
4. Verify GitHub storage

---

## Roadmap

### Phase 1: Foundation ✅
- [x] Authentication
- [x] File upload
- [x] Document parsing
- [x] GitHub storage

### Phase 2: AI Integration (Next)
- [ ] OpenAI/Claude integration
- [ ] Agent orchestration
- [ ] Async processing with Redis

### Phase 3: Artifact Generation
- [ ] WBS agent
- [ ] User stories agent
- [ ] Tech specs agent
- [ ] NFR agent
- [ ] Architecture agent
- [ ] Sprint planning agent
- [ ] Test case agent

### Phase 4: Production Ready
- [ ] PostgreSQL integration
- [ ] JWT authentication
- [ ] WebSocket updates
- [ ] Monitoring & logging
- [ ] Docker deployment
- [ ] Kubernetes orchestration

---

## License

This project is for educational/demonstration purposes.

---

## Contact

- **GitHub Repository**: https://github.com/Rachit19000/files_storage
- **Email**: rachitjainemail@gmail.com

---

## Quick Commands

```bash
# Java Backend
cd backend-java && mvn spring-boot:run

# Node.js Backend  
cd backend && npm start

# Frontend
cd frontend && npm start

# Full Stack (Java)
# Terminal 1: cd backend-java && mvn spring-boot:run
# Terminal 2: cd frontend && npm start
```

---

**Built with ❤️ using React, Spring Boot, and AI**
