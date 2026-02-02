# Quick Start Guide - Java Backend

## Prerequisites Check

1. Java installed?
   ```bash
   java -version
   ```
   If not: Download from https://adoptium.net/

2. Maven installed?
   ```bash
   mvn -version
   ```
   If not: Download from https://maven.apache.org/download.cgi

---

## Setup in 5 Steps

### Step 1: Set GitHub Token

Create `backend-java/.env`:
```env
GITHUB_TOKEN=your_github_token_here
```

Or set environment variable:
```powershell
$env:GITHUB_TOKEN="your_github_token_here"
```

### Step 2: Build

```bash
cd backend-java
mvn clean install
```

Wait for: `BUILD SUCCESS`

### Step 3: Run

```bash
mvn spring-boot:run
```

Wait for: `🚀 SDLC Backend running on http://localhost:3001/api/v1`

### Step 4: Test

Open: http://localhost:3001/api/v1/auth/health

You should see:
```json
{"status":"ok","message":"Backend API is running"}
```

### Step 5: Use Frontend

1. Frontend should already be running on `http://localhost:3000`
2. Login: `rachitjainemail@gmail.com` / `password123`
3. Upload files — they'll be parsed and stored in GitHub!

---

## Stopping the Server

Press `Ctrl+C` in the terminal

---

## Troubleshooting

### "GITHUB_TOKEN is required"
→ Set the environment variable (see Step 1)

### "Port 3001 already in use"
→ Stop Node.js backend if running

### Maven build fails
→ Run: `mvn clean install -U`

---

## Quick Commands

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Build JAR
mvn package

# Run JAR
java -jar target/sdlc-automation-backend-1.0.0.jar
```

That's it! The Java backend is running.
