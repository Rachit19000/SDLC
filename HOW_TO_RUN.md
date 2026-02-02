# How to Run the SDLC Automation Platform

## Overview
- **Frontend**: React app running on `http://localhost:3000`
- **Backend**: Java Spring Boot app running on `http://localhost:3001`

---

## Prerequisites

1. **Node.js and npm** (for frontend)
   - Check: `node --version` and `npm --version`
   - Install from: https://nodejs.org/

2. **Java JDK 17+** (for backend)
   - Check: `java -version`
   - Install from: https://adoptium.net/

3. **Maven** (for backend)
   - Check: `mvn -version`
   - Install from: https://maven.apache.org/download.cgi

4. **GitHub Personal Access Token**
   - Create at: https://github.com/settings/tokens
   - Required scope: `repo`

---

## Step 1: Set Up GitHub Token

### Option A: Environment Variable (Recommended)
Open PowerShell and run:
```powershell
$env:GITHUB_TOKEN = "your_github_token_here"
```

### Option B: Create .env File
Create `backend/.env` file:
```
GITHUB_TOKEN=your_github_token_here
```

---

## Step 2: Run the Backend (Java Spring Boot)

### Method 1: Using the PowerShell Script (Easiest)
```powershell
cd backend-java
.\run-backend.ps1
```

### Method 2: Manual Command
```powershell
cd backend-java
$env:GITHUB_TOKEN = "your_github_token_here"
mvn spring-boot:run
```

**Wait for:** You should see:
```
Started SdlcBackendApplication in X.XXX seconds
```

**Backend URL:** `http://localhost:3001/api/v1`

---

## Step 3: Run the Frontend (React)

Open a **NEW terminal window** (keep backend running):

```powershell
cd frontend
npm start
```

**Wait for:** Browser should automatically open to `http://localhost:3000`

---

## Step 4: Test the Application

1. **Login Page**: `http://localhost:3000`
   - Email: `rachitjainemail@gmail.com`
   - Password: `password123`

2. **After Login**: You'll be redirected to the Dashboard where you can upload requirements.

---

## Troubleshooting

### Backend won't start - "GITHUB_TOKEN is required"
**Solution:** Set the GitHub token:
```powershell
$env:GITHUB_TOKEN = "your_token_here"
```

### Backend won't start - "mvn not recognized"
**Solution:** Add Maven to PATH or use the `run-backend.ps1` script.

### Frontend shows "Cannot connect to server"
**Solution:** 
1. Make sure backend is running on port 3001
2. Check: `curl http://localhost:3001/api/v1/auth/login`

### Port already in use
**Solution:** Kill the process using the port:
```powershell
# For port 3001 (backend)
netstat -ano | findstr :3001
taskkill /PID <PID_NUMBER> /F

# For port 3000 (frontend)
netstat -ano | findstr :3000
taskkill /PID <PID_NUMBER> /F
```

---

## Quick Start (All Commands)

### Terminal 1 - Backend:
```powershell
cd backend-java
$env:GITHUB_TOKEN = "your_github_token_here"
mvn spring-boot:run
```

### Terminal 2 - Frontend:
```powershell
cd frontend
npm start
```

---

## Directory Structure

```
SDLC/
├── frontend/          # React app (port 3000)
│   └── npm start
│
└── backend-java/      # Spring Boot app (port 3001)
    └── mvn spring-boot:run
```

---

## Notes

- **Backend must be running** before you can login
- **Both servers** must run simultaneously
- **GitHub token** is required for file uploads to work
- Use **Ctrl+C** to stop either server
