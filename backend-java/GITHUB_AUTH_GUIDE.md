# GitHub Authentication Guide

This guide explains how to use GitHub authentication in the SDLC Platform.

---

## Two Ways to Authenticate with GitHub

### Option 1: Regular Login (Email/Password)
- Use the **"Regular Login"** tab
- Enter your SDLC Platform email and password
- Default test credentials:
  - Email: `rachitjainemail@gmail.com`
  - Password: `password123`

### Option 2: GitHub Authentication (Username/Token)
- Use the **"GitHub Login"** tab
- Enter your **GitHub username** and **Personal Access Token**
- The backend verifies your credentials directly with GitHub API

---

## How to Get a GitHub Personal Access Token

### Step 1: Go to GitHub Settings
1. Visit: https://github.com/settings/tokens
2. Click **"Generate new token"** → **"Generate new token (classic)"**

### Step 2: Configure Token
3. Fill in the details:
   - **Note**: `SDLC Platform Authentication`
   - **Expiration**: 90 days (or your preference)
   - **Scopes**: Check the following:
     - ✅ `user:email` - Access user email addresses
     - ✅ `read:user` - Read user profile data
     - ✅ `repo` - Full control of repositories (for file storage)

### Step 3: Generate and Copy
4. Click **"Generate token"**
5. **IMPORTANT:** Copy the token immediately (starts with `ghp_...`)
   - Example: `ghp_ABCdefGHIjklMNOpqrSTUvwxYZ1234567890`
6. Save it securely - you won't see it again!

---

## How to Login with GitHub

### Frontend (Login Page)
1. Open: http://localhost:3000
2. Click the **"GitHub Login"** tab
3. Enter your credentials:
   - **GitHub username or email**: Your GitHub username (e.g., `Rachit19000`)
   - **GitHub password or token**: Your Personal Access Token (e.g., `ghp_ABC...`)
4. Click **"Sign in"**
5. You'll be authenticated via GitHub API!

### What Happens
```
1. User enters GitHub username and token
   ↓
2. Frontend sends to: POST /api/v1/auth/github-login
   ↓
3. Backend validates with GitHub API
   ↓
4. GitHub API returns user info (name, email, ID)
   ↓
5. Backend creates session and returns token
   ↓
6. User is logged in!
```

---

## API Endpoint

### GitHub Login Endpoint
```http
POST /api/v1/auth/github-login
Content-Type: application/json

{
  "username": "Rachit19000",
  "password": "ghp_ABCdefGHIjklMNOpqrSTUvwxYZ1234567890"
}

Response: 200 OK
{
  "token": "github_token_12345678_1234567890",
  "user": {
    "id": "user_github_12345678",
    "email": "rachitjainemail@gmail.com",
    "name": "Rachit Jain"
  }
}
```

---

## User Information Retrieved

When you authenticate with GitHub, the backend retrieves:

- **GitHub ID**: Your unique GitHub identifier
- **Username**: Your GitHub username (login)
- **Name**: Your full name from GitHub profile
- **Email**: Your primary email from GitHub account
- **Profile**: Additional profile information

---

## Security Notes

### ✅ DO:
- Use Personal Access Tokens instead of passwords
- Set appropriate token scopes (minimal required)
- Set token expiration dates
- Store tokens securely
- Revoke tokens when not needed

### ❌ DON'T:
- Share your Personal Access Token
- Commit tokens to Git repositories
- Use tokens with excessive permissions
- Use the same token across multiple apps

---

## Troubleshooting

### Error: "Invalid GitHub credentials"
**Causes:**
- Token is incorrect or expired
- Token doesn't have required scopes
- GitHub username is wrong

**Solution:**
1. Verify your GitHub username: https://github.com/YOUR_USERNAME
2. Generate a new Personal Access Token
3. Ensure token has `user:email` and `read:user` scopes
4. Try again with the new token

### Error: "Failed to authenticate with GitHub"
**Causes:**
- GitHub API is down
- Network connectivity issues
- Token was revoked

**Solution:**
1. Check GitHub status: https://www.githubstatus.com/
2. Verify internet connection
3. Generate a new token and try again

### Error: "Cannot connect to server"
**Cause:**
- Backend is not running

**Solution:**
```powershell
cd backend-java
$env:GITHUB_TOKEN = "your_token_here"
mvn spring-boot:run
```

---

## Comparison: Regular vs GitHub Login

| Feature | Regular Login | GitHub Login |
|---------|---------------|--------------|
| **Credentials** | SDLC email/password | GitHub username/token |
| **Authentication** | Local database | GitHub API |
| **User Info** | Stored locally | Fetched from GitHub |
| **Security** | Platform-managed | GitHub-managed |
| **Best For** | Internal users | GitHub developers |

---

## Why Use Personal Access Tokens?

GitHub deprecated **password authentication** for security reasons. Personal Access Tokens provide:

1. **Better Security**: Tokens can be scoped with specific permissions
2. **Revocability**: Can be revoked without changing password
3. **Expiration**: Tokens can expire automatically
4. **Audit Trail**: GitHub logs token usage
5. **API Access**: Required for GitHub API operations

---

## Example: Complete Flow

```bash
# 1. Generate token at: https://github.com/settings/tokens
#    Token: ghp_ABC123def456GHI789jkl012MNO345

# 2. Open login page: http://localhost:3000

# 3. Click "GitHub Login" tab

# 4. Enter:
#    Username: Rachit19000
#    Password/Token: ghp_ABC123def456GHI789jkl012MNO345

# 5. Click "Sign in"

# 6. Success! You're logged in with GitHub credentials
```

---

## Testing

### Test with Your GitHub Account
1. Generate a Personal Access Token
2. Use the token in the login form
3. Verify successful authentication

### Test Regular Login (No Token Needed)
1. Click "Regular Login" tab
2. Email: `rachitjainemail@gmail.com`
3. Password: `password123`
4. Login works without GitHub token

---

## Next Steps

After implementing GitHub authentication:

1. ✅ **JWT Tokens**: Replace mock tokens with secure JWT
2. ✅ **Database Storage**: Store user sessions in PostgreSQL
3. ✅ **Token Refresh**: Implement refresh token mechanism
4. ✅ **Multi-Factor Auth**: Add 2FA support
5. ✅ **SSO**: Support other OAuth providers (Google, Microsoft)

---

## Support

If you encounter issues:
1. Check backend logs for detailed error messages
2. Verify your GitHub token at: https://github.com/settings/tokens
3. Ensure the backend is running on `http://localhost:3001`
4. Test token validity manually: `curl -H "Authorization: token YOUR_TOKEN" https://api.github.com/user`

---

## Quick Reference

| Action | URL/Command |
|--------|-------------|
| Generate Token | https://github.com/settings/tokens |
| Login Page | http://localhost:3000 |
| Backend API | http://localhost:3001/api/v1 |
| Test Token | `curl -H "Authorization: token TOKEN" https://api.github.com/user` |
| Start Backend | `cd backend-java && mvn spring-boot:run` |
| Start Frontend | `cd frontend && npm start` |
