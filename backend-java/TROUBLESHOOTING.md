# Backend Troubleshooting Guide

## Issue: 404 Errors on All Endpoints

### Quick Fix Steps:

1. **Check if backend is actually running:**
   ```powershell
   netstat -ano | findstr :3001
   ```
   Should show LISTENING on port 3001

2. **Test the endpoint directly:**
   ```powershell
   curl http://localhost:3001/api/v1/auth/health
   ```
   Or in PowerShell:
   ```powershell
   Invoke-WebRequest -Uri "http://localhost:3001/api/v1/auth/health"
   ```

3. **Check backend logs:**
   Look for:
   - "Started SdlcBackendApplication"
   - Any error messages
   - Port binding issues

4. **Verify context path:**
   In `application.properties`:
   ```
   server.servlet.context-path=/api/v1
   ```
   This means all endpoints are prefixed with `/api/v1`

5. **Restart backend:**
   ```powershell
   # Stop (Ctrl+C)
   cd backend-java
   mvn clean spring-boot:run
   ```

### Common Issues:

#### Issue 1: Port Already in Use
```powershell
# Find process using port 3001
netstat -ano | findstr :3001
# Kill the process (replace PID with actual process ID)
taskkill /PID <PID> /F
```

#### Issue 2: Context Path Not Working
If endpoints still return 404, try accessing without context path:
```powershell
# Test without /api/v1
curl http://localhost:3001/auth/health
```

#### Issue 3: Spring Security Blocking
The SecurityConfig has been updated to allow all requests. Make sure it's compiled:
```powershell
mvn clean compile
mvn spring-boot:run
```

#### Issue 4: GitHub Authentication Not Working
The GitHub authentication service requires:
- Valid GitHub username
- Personal Access Token (not password - GitHub deprecated password auth)

Get token at: https://github.com/settings/tokens

### Testing GitHub Login Endpoint:

```powershell
$body = @{
    username = "YourGitHubUsername"
    password = "ghp_YOUR_TOKEN_HERE"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:3001/api/v1/auth/github-login" `
    -Method POST `
    -ContentType "application/json" `
    -Body $body
```

### Expected Response:
```json
{
  "token": "github_token_12345678_1234567890",
  "user": {
    "id": "user_github_12345678",
    "email": "user@example.com",
    "name": "User Name"
  }
}
```

### If Still Not Working:

1. Check backend console for errors
2. Verify Maven dependencies are installed: `mvn dependency:tree`
3. Check if GitHub API library is working: Look for `org.kohsuke.github` in dependencies
4. Try accessing a simple endpoint first: `/api/v1/auth/health`
