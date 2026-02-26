# Quick Start: GitHub Integration for Cursor

Get your GitHub documents (user stories, requirements, tech specs) opened in Cursor in 3 steps!

## Step 1: Install Dependencies

```bash
pip install -r requirements.txt
```

## Step 2: Configure GitHub Access

Create a `.env` file in this directory with your GitHub credentials:

```env
GITHUB_TOKEN=your_github_personal_access_token
GITHUB_REPO_OWNER=your_username_or_org
GITHUB_REPO_NAME=your_repository_name
GITHUB_BRANCH=main
```

**To get a GitHub token:**
1. Go to https://github.com/settings/tokens
2. Click "Generate new token" → "Generate new token (classic)"
3. Select scope: `repo` (for private) or `public_repo` (for public)
4. Copy the token to your `.env` file

## Step 3: Use It!

### Option A: Quick Command (Recommended)

```bash
# Open user stories
python fetch_github_docs.py user-stories

# Open requirements
python fetch_github_docs.py requirements

# Open tech specs
python fetch_github_docs.py tech-specs

# Open all documents
python fetch_github_docs.py all

# List available documents
python fetch_github_docs.py list
```

### Option B: PowerShell Script (Windows)

```powershell
.\fetch-github-docs.ps1 user-stories
.\fetch-github-docs.ps1 requirements
.\fetch-github-docs.ps1 tech-specs
.\fetch-github-docs.ps1 all
.\fetch-github-docs.ps1 list
```

### Option C: Python Code

```python
from utils import open_github_docs

# Open user stories
files = open_github_docs('user-stories')

# Or use the integration module directly
from github_integration import open_user_stories
files = open_user_stories()
```

## That's It! 🎉

Your GitHub documents will be:
1. Downloaded to `github_docs/` folder in your workspace
2. Automatically opened in Cursor IDE

## Need Help?

See `GITHUB_INTEGRATION_README.md` for detailed documentation.
