# QUICK FIX: Backend 404 Error

## Problem
Backend is running but returning 404 for all endpoints.

## Solution: RESTART BACKEND

The backend needs to be restarted after SecurityConfig changes.

### Steps:

1. **Stop the backend** (Press `Ctrl+C` in the terminal where backend is running)

2. **Restart it:**
   ```powershell
   cd backend-java
   mvn spring-boot:run
   ```

3. **Wait for this message:**
   ```
   Started SdlcBackendApplication in X.XXX seconds
   ```

4. **Test the endpoint:**
   ```powershell
   Invoke-WebRequest -Uri "http://localhost:3001/api/v1/auth/health"
   ```
   Should return: `{"status":"ok","message":"Backend API is running"}`

5. **Try login again in frontend**

## If Still Not Working:

### Check 1: Verify Backend is Running
```powershell
netstat -ano | findstr :3001
```
Should show LISTENING

### Check 2: Test Endpoint Directly
```powershell
$body = @{username="test";password="test"} | ConvertTo-Json
Invoke-WebRequest -Uri "http://localhost:3001/api/v1/auth/github-login" -Method POST -ContentType "application/json" -Body $body
```

### Check 3: Check Backend Logs
Look for errors in the backend console output.

### Check 4: Verify application.properties
Make sure `server.servlet.context-path=/api/v1` is set.

## Common Issues:

1. **Port already in use**: Kill the process and restart
2. **Maven dependencies**: Run `mvn clean install`
3. **Java version**: Make sure Java 17+ is installed
4. **Context path mismatch**: Verify `/api/v1` is in application.properties
