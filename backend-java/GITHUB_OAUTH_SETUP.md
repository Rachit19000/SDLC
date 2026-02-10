# GitHub OAuth Setup Guide

This guide explains how to set up GitHub OAuth authentication for the SDLC Automation Platform.

---

## Step 1: Create a GitHub OAuth App

1. Go to GitHub: https://github.com/settings/developers
2. Click **"New OAuth App"** or **"Register a new application"**
3. Fill in the application details:
   - **Application name**: `SDLC Automation Platform`
   - **Homepage URL**: `http://localhost:3000`
   - **Authorization callback URL**: `http://localhost:3001/api/v1/oauth2/callback/github`
4. Click **"Register application"**
5. On the next page, you'll see:
   - **Client ID** (e.g., `Ov23li...`)
   - Click **"Generate a new client secret"** to get the **Client Secret**
6. **IMPORTANT:** Copy both the Client ID and Client Secret immediately!

---

## Step 2: Configure Backend

You need to set two environment variables for GitHub OAuth:

### Option A: Environment Variables (Recommended)

**Windows PowerShell:**
```powershell
$env:GITHUB_OAUTH_CLIENT_ID = "your_client_id_here"
$env:GITHUB_OAUTH_CLIENT_SECRET = "your_client_secret_here"
$env:GITHUB_TOKEN = "your_github_personal_access_token"
```

**Linux/Mac:**
```bash
export GITHUB_OAUTH_CLIENT_ID="your_client_id_here"
export GITHUB_OAUTH_CLIENT_SECRET="your_client_secret_here"
export GITHUB_TOKEN="your_github_personal_access_token"
```

### Option B: Update application.properties

Edit `backend-java/src/main/resources/application.properties`:
```properties
spring.security.oauth2.client.registration.github.client-id=your_client_id_here
spring.security.oauth2.client.registration.github.client-secret=your_client_secret_here
```

**⚠️ WARNING:** Do not commit these values to Git! Add `application.properties` to `.gitignore` if you use this method.

---

## Step 3: Start the Backend

After setting the environment variables, start the backend:

```powershell
cd backend-java
mvn spring-boot:run
```

You should see:
```
Started SdlcBackendApplication in X.XXX seconds
```

---

## Step 4: Test OAuth Login

1. Open frontend: http://localhost:3000
2. Click **"Sign in with GitHub"** button
3. You'll be redirected to GitHub's authorization page
4. Click **"Authorize"** to grant access
5. You'll be redirected back to the dashboard

---

## How It Works

```
1. User clicks "Sign in with GitHub"
   ↓
2. Frontend redirects to: http://localhost:3001/oauth2/authorization/github
   ↓
3. Backend redirects to GitHub OAuth page
   ↓
4. User authorizes the app on GitHub
   ↓
5. GitHub redirects back to: http://localhost:3001/api/v1/oauth2/callback/github
   ↓
6. Backend exchanges code for access token
   ↓
7. Backend fetches user info from GitHub API
   ↓
8. Backend creates session and redirects to: http://localhost:3000/dashboard
   ↓
9. User is logged in!
```

---

## User Information Retrieved

When a user signs in with GitHub, the backend retrieves:

- **GitHub ID**: Unique identifier
- **Username**: GitHub username (login)
- **Name**: Full name
- **Email**: Primary email address
- **Avatar URL**: Profile picture URL

This information is stored in the session and can be used throughout the application.

---

## API Endpoints

### OAuth Endpoints

- **Start OAuth Flow**: `GET /oauth2/authorization/github`
  - Redirects to GitHub for authorization

- **OAuth Callback**: `GET /api/v1/oauth2/callback/github`
  - Handles the callback from GitHub
  - Automatically called by Spring Security

- **Get Current User**: `GET /api/v1/oauth2/user`
  - Returns authenticated user information
  - Requires OAuth2 authentication

- **Health Check**: `GET /api/v1/oauth2/health`
  - Check if OAuth2 endpoints are available

---

## Troubleshooting

### Error: "invalid_client"
- Client ID or Client Secret is incorrect
- Make sure environment variables are set correctly
- Restart the backend after setting variables

### Error: "redirect_uri_mismatch"
- The callback URL in GitHub OAuth App settings doesn't match
- Update it to: `http://localhost:3001/api/v1/oauth2/callback/github`

### Error: "unauthorized_client"
- The GitHub OAuth App might be suspended
- Check your GitHub OAuth App settings

### Backend shows "Forbidden" or 401 errors
- Spring Security is blocking requests
- Check SecurityConfig.java to ensure endpoints are permitted

---

## Security Notes

1. **Never commit** Client ID and Client Secret to Git
2. **Use environment variables** for all sensitive credentials
3. In production:
   - Use HTTPS for all OAuth redirects
   - Store secrets in a secrets manager (AWS Secrets Manager, HashiCorp Vault)
   - Implement proper JWT token generation instead of mock tokens
   - Add CSRF protection
   - Implement rate limiting

---

## Testing OAuth Without GitHub Account

If you want to test without setting up GitHub OAuth:

1. Use the regular email/password login
2. Email: `rachitjainemail@gmail.com`
3. Password: `password123`

---

## Next Steps

After OAuth is working:

1. **Implement JWT tokens** instead of mock tokens
2. **Store user data** in a database (PostgreSQL)
3. **Add refresh tokens** for long-lived sessions
4. **Implement logout** endpoint
5. **Add role-based access control** (RBAC)
6. **Support multiple OAuth providers** (Google, Microsoft, etc.)

---

## Complete Example

```powershell
# 1. Set all required environment variables
$env:GITHUB_OAUTH_CLIENT_ID = "Ov23li1A2B3C4D5E6F7G"
$env:GITHUB_OAUTH_CLIENT_SECRET = "abc123def456ghi789jkl012mno345pqr678stu"
$env:GITHUB_TOKEN = "ghp_ABCdefGHIjklMNOpqrSTUvwxYZ1234567890"

# 2. Start backend
cd backend-java
mvn spring-boot:run

# 3. In a new terminal, start frontend
cd frontend
npm start

# 4. Open browser: http://localhost:3000
# 5. Click "Sign in with GitHub"
# 6. Authorize on GitHub
# 7. You're logged in!
```

---

## Support

If you encounter issues:
1. Check the backend logs for error messages
2. Verify environment variables are set: `echo $env:GITHUB_OAUTH_CLIENT_ID`
3. Ensure GitHub OAuth App callback URL is correct
4. Check Spring Security logs: `logging.level.org.springframework.security=DEBUG`
