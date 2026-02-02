# Java Backend Installation Guide

## Step-by-Step Installation

### Step 1: Install Java 17

#### Windows
1. Download from: https://adoptium.net/temurin/releases/?version=17
2. Choose: Windows x64 installer
3. Run installer → Next → Next → Install
4. Verify:
   ```powershell
   java -version
   ```
   Should show: `openjdk version "17.x.x"`

#### Mac
```bash
brew install openjdk@17
```

#### Linux
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

---

### Step 2: Install Maven

#### Windows
1. Download from: https://maven.apache.org/download.cgi
2. Extract ZIP to `C:\Program Files\Apache\maven`
3. Add to PATH:
   - System Properties → Environment Variables
   - Add to Path: `C:\Program Files\Apache\maven\bin`
4. Verify:
   ```powershell
   mvn -version
   ```

#### Mac
```bash
brew install maven
```

#### Linux
```bash
sudo apt update
sudo apt install maven
```

---

### Step 3: Set GitHub Token

#### Option A: Environment Variable (Recommended)

**Windows PowerShell:**
```powershell
$env:GITHUB_TOKEN="your_github_token_here"
```

**Windows Command Prompt:**
```cmd
set GITHUB_TOKEN=your_github_token_here
```

**Linux/Mac:**
```bash
export GITHUB_TOKEN=your_github_token_here
```

#### Option B: application.properties

Edit `src/main/resources/application.properties`:
```properties
github.token=your_github_token_here
```

**⚠️ Warning:** Don't commit this to Git!

---

### Step 4: Build the Project

```bash
cd backend-java
mvn clean install
```

**Expected output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time:  45.678 s
```

**If build fails:**
- Check Java version: `java -version` (must be 17+)
- Check Maven: `mvn -version`
- Run: `mvn clean install -U` (force update dependencies)

---

### Step 5: Run the Application

```bash
mvn spring-boot:run
```

**Expected output:**
```
🚀 SDLC Backend running on http://localhost:3001/api/v1
📝 Test login with: rachitjainemail@gmail.com / password123
```

---

## Verify Installation

### Test 1: Health Check

Open browser: http://localhost:3001/api/v1/auth/health

**Expected:**
```json
{"status":"ok","message":"Backend API is running"}
```

### Test 2: Login via Curl

```bash
curl -X POST http://localhost:3001/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"rachitjainemail@gmail.com","password":"password123"}'
```

**Expected:**
```json
{
  "token": "mock_token_user_1_...",
  "user": {
    "id": "user_1",
    "email": "rachitjainemail@gmail.com",
    "name": "Rachit Jain"
  }
}
```

### Test 3: Frontend Integration

1. Frontend running on http://localhost:3000
2. Login with: `rachitjainemail@gmail.com` / `password123`
3. Upload a PDF or DOCX file
4. Check GitHub: https://github.com/Rachit19000/files_storage
   - Should see extracted text in `requirements/` folder

---

## Common Issues

### "JAVA_HOME not set"
```bash
# Windows
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17.0.x"

# Linux/Mac
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
```

### "GITHUB_TOKEN is required"
- Set environment variable (see Step 3)
- Or add to `application.properties`

### "Port 3001 already in use"
Stop Node.js backend:
```bash
# Find process
netstat -ano | findstr :3001

# Kill process (replace PID)
taskkill /PID <PID> /F
```

Or change port in `application.properties`:
```properties
server.port=8080
```

### "Cannot resolve dependencies"
```bash
mvn clean install -U
```

### "Class not found"
```bash
mvn clean install
mvn spring-boot:run
```

---

## IDE Setup

### IntelliJ IDEA (Recommended)
1. Open → `backend-java` folder
2. Trust Maven project
3. Wait for dependencies to download
4. Run → SdlcBackendApplication
5. Or use Maven panel → spring-boot:run

### Eclipse
1. Import → Existing Maven Project
2. Select `backend-java` folder
3. Right-click project → Run As → Spring Boot App

### VS Code
1. Install Extension Pack for Java
2. Open `backend-java` folder
3. F5 to run

---

## Development Commands

### Build
```bash
mvn clean install
```

### Run (Development)
```bash
mvn spring-boot:run
```

### Run (Production JAR)
```bash
mvn package
java -jar target/sdlc-automation-backend-1.0.0.jar
```

### Clean
```bash
mvn clean
```

### Update Dependencies
```bash
mvn clean install -U
```

### Run Tests
```bash
mvn test
```

---

## Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `GITHUB_TOKEN` | GitHub Personal Access Token | Yes |
| `SERVER_PORT` | Server port (default: 3001) | No |

---

## Next Steps

1. ✅ Java installed
2. ✅ Maven installed
3. ✅ GitHub token set
4. ✅ Project built
5. ✅ Application running
6. → Test with frontend
7. → Upload documents
8. → Verify GitHub storage

You're ready to use the Java backend!
