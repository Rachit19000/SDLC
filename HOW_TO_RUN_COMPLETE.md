# Complete Guide: How to Run Frontend and Backend

## 📍 Where to Run Commands
**Answer: Run all commands in Cursor's integrated PowerShell terminal** (the terminal panel at the bottom of Cursor)

---

## 🚀 Step-by-Step Instructions

### **STEP 1: Set Up Environment Variables (One-Time Setup)**

Open Cursor's PowerShell terminal and run:

```powershell
# Set Java and Maven paths
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"
$env:M2_HOME = "C:\Program Files\apache-maven-3.9.12-bin\apache-maven-3.9.12"
$env:PATH = "$env:M2_HOME\bin;$env:PATH"

# Verify Maven works
mvn -version
```

**Expected Output:** You should see Maven version information.

---

### **STEP 2: Configure GitHub Token**

1. Open file: `backend-java/src/main/resources/application.properties`
2. Find the line: `github.token=${GITHUB_TOKEN:}`
3. Keep it as is (it reads from environment variable)

   **OR** set it as environment variable:
   ```powershell
   $env:GITHUB_TOKEN = "your_github_token_here"
   ```

---

### **STEP 3: Start the Backend (Java/Spring Boot)**

**In Cursor's PowerShell terminal:**

```powershell
# Navigate to backend directory
cd backend-java

# Start Spring Boot backend
mvn spring-boot:run
```

**What to expect:**
- Maven will download dependencies (first time only, takes 1-2 minutes)
- You'll see Spring Boot starting up
- Look for: `Started SdlcBackendApplication` message
- Backend runs on: `http://localhost:3001`
- API endpoints available at: `http://localhost:3001/api/v1`

**Keep this terminal window open!** The backend must keep running.

---

### **STEP 4: Start the Frontend (React)**

**Open a NEW terminal tab/window in Cursor** (click the `+` button to open a new terminal):

```powershell
# Navigate to frontend directory
cd frontend

# Install dependencies (only needed first time)
npm install

# Start React development server
npm start
```

**What to expect:**
- React will compile
- Browser should automatically open to `http://localhost:3000`
- If not, manually open: `http://localhost:3000`

**Keep this terminal window open too!** The frontend must keep running.

---

## 📋 Complete Startup Checklist

### Terminal 1: Backend (Java)
```powershell
# Set environment variables
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"
$env:M2_HOME = "C:\Program Files\apache-maven-3.9.12-bin\apache-maven-3.9.12"
$env:PATH = "$env:M2_HOME\bin;$env:PATH"

# Start backend
cd backend-java
mvn spring-boot:run
```

### Terminal 2: Frontend (React)
```powershell
cd frontend
npm start
```

---

## ✅ Verify Everything is Running

1. **Backend Health Check:**
   - Open browser: `http://localhost:3001/api/v1/auth/health`
   - Should see: `{"status":"ok","message":"Backend API is running"}`

2. **Frontend:**
   - Open browser: `http://localhost:3000`
   - Should see the login page

3. **Test Login:**
   - Email: `rachitjainemail@gmail.com`
   - Password: `password123`

---

## 🛑 How to Stop

- **Backend:** Press `Ctrl + C` in the backend terminal
- **Frontend:** Press `Ctrl + C` in the frontend terminal

---

## 🔧 Troubleshooting

### Maven not found?
- Make sure you set `$env:M2_HOME` and `$env:PATH` in the same terminal session
- Or use the helper script: `cd backend-java; .\run-backend.ps1`

### Port already in use?
- Backend (3001): Check if another process is using port 3001
- Frontend (3000): Check if another process is using port 3000
- Kill process: `netstat -ano | findstr :3001` then `taskkill /PID <pid> /F`

### Frontend can't connect to backend?
- Make sure backend is running on port 3001
- Check CORS settings in `application.properties`
- Verify backend URL in frontend code: `http://localhost:3001/api/v1`

### GitHub upload not working?
- Verify GitHub token is set correctly
- Check token has `repo` permissions
- Look at backend logs for error messages

---

## 📝 Quick Reference

| Component | Port | URL | Command |
|-----------|------|-----|---------|
| **Backend** | 3001 | http://localhost:3001/api/v1 | `mvn spring-boot:run` |
| **Frontend** | 3000 | http://localhost:3000 | `npm start` |

---

## 💡 Pro Tips

1. **Use Cursor's split terminal:** You can split the terminal to see both backend and frontend logs side-by-side
2. **Save environment variables:** Consider adding Maven to your system PATH permanently (see previous instructions)
3. **Use the helper script:** `backend-java/run-backend.ps1` automates backend startup

---

## 🎯 Summary

1. ✅ Set environment variables (JAVA_HOME, M2_HOME, PATH)
2. ✅ Configure GitHub token
3. ✅ Start backend in Terminal 1: `cd backend-java; mvn spring-boot:run`
4. ✅ Start frontend in Terminal 2: `cd frontend; npm start`
5. ✅ Open browser: http://localhost:3000
6. ✅ Login and test!

**All commands run in Cursor's integrated PowerShell terminal!**
