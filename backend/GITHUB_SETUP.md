# GitHub Storage Setup Guide

## Overview
This setup stores uploaded files/text in the user's GitHub repository: `https://github.com/Rachit19000/files_storage`

---

## Step 1: Create GitHub Personal Access Token

1. Go to GitHub: https://github.com/settings/tokens
2. Click **"Generate new token"** → **"Generate new token (classic)"**
3. Fill in:
   - **Note**: `SDLC File Storage`
   - **Expiration**: Choose your preference (90 days recommended)
   - **Scopes**: Check **`repo`** (Full control of private repositories)
4. Click **"Generate token"**
5. **IMPORTANT:** Copy the token immediately (you won't see it again!)

---

## Step 2: Configure Backend

### Option A: Environment Variable (Recommended)

Create `backend/.env` file:
```env
GITHUB_TOKEN=your_github_token_here
```

### Option B: Direct Configuration

Edit `backend/github-config.js`:
- Replace `YOUR_GITHUB_TOKEN_HERE` with your actual token

---

## Step 3: Verify Repository Access

Make sure:
- The token has access to `Rachit19000/files_storage` repo
- The repo exists and you have write access
- The default branch is `main` (or update in `github-config.js`)

---

## Step 4: Test the Setup

1. Start backend:
   ```bash
   cd backend
   npm start
   ```

2. Login with: `rachitjainemail@gmail.com` / `password123`

3. Upload text or file

4. Check GitHub repo: `https://github.com/Rachit19000/files_storage`
   - Files will be in: `requirements/user_1/job_XXXXX/`

---

## Folder Structure in GitHub

```
files_storage/
  └── requirements/
      └── user_1/
          └── job_1234567890/
              └── requirement.txt (or uploaded file)
```

---

## How It Works

1. **User logs in** → Gets token
2. **User uploads text/file** → Backend receives it
3. **Backend creates file in GitHub**:
   - Path: `requirements/{userId}/{jobId}/{fileName}`
   - Commits with message: "Add requirement: {jobId}"
4. **Returns GitHub URL** to frontend
5. **File is now in GitHub repo!**

---

## Troubleshooting

### Error: "No GitHub repository configured"
- Check `github-config.js` - make sure email matches exactly
- Email must be: `rachitjainemail@gmail.com`

### Error: "Bad credentials"
- Token is invalid or expired
- Generate new token and update `.env` or `github-config.js`

### Error: "Resource not accessible"
- Token doesn't have `repo` scope
- Regenerate token with `repo` permission

### Error: "Not found"
- Repository doesn't exist or token doesn't have access
- Verify repo: `https://github.com/Rachit19000/files_storage`

---

## Security Notes

1. **Never commit** `.env` file to Git
2. Add `.env` to `.gitignore`
3. Token gives full repo access - keep it secret!
4. In production, use GitHub OAuth instead of personal tokens

---

## Adding More Users

Edit `backend/github-config.js`:
```javascript
const userRepos = {
  'rachitjainemail@gmail.com': {
    owner: 'Rachit19000',
    repo: 'files_storage',
    branch: 'main'
  },
  'other@example.com': {
    owner: 'otherusername',
    repo: 'other_repo',
    branch: 'main'
  }
};
```

---

## Success Response

When file is uploaded, you'll get:
```json
{
  "jobId": "job_1234567890",
  "status": "stored",
  "message": "Text uploaded successfully to GitHub",
  "githubUrl": "https://github.com/Rachit19000/files_storage/blob/main/requirements/user_1/job_123/file.txt",
  "commitUrl": "https://github.com/Rachit19000/files_storage/commit/abc123",
  "filePath": "requirements/user_1/job_123/file.txt",
  "fileName": "file.txt"
}
```
