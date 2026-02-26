# GitHub Integration for Cursor IDE

This module allows you to fetch and open files from your GitHub repository directly in Cursor IDE. Specifically designed to work with user stories, user requirements, and technical specifications documents.

## Features

- ✅ Fetch files from GitHub repositories
- ✅ Automatically detect and filter document types (user stories, requirements, tech specs)
- ✅ Download files locally and open them in Cursor IDE
- ✅ Interactive file selection
- ✅ Command-line interface for quick access

## Setup

### 1. Install Dependencies

```bash
pip install -r requirements.txt
```

### 2. Configure GitHub Access

Create a `.env` file in this directory (or copy from `.env.example`):

```bash
# Copy the example file
cp .env.example .env
```

Edit `.env` and add your GitHub credentials:

```env
GITHUB_TOKEN=your_github_personal_access_token
GITHUB_REPO_OWNER=your_username_or_org
GITHUB_REPO_NAME=your_repository_name
GITHUB_BRANCH=main  # Optional, defaults to 'main'
```

### 3. Create GitHub Personal Access Token

1. Go to https://github.com/settings/tokens
2. Click "Generate new token" → "Generate new token (classic)"
3. Give it a name (e.g., "Cursor Integration")
4. Select scopes:
   - For **public repos**: `public_repo`
   - For **private repos**: `repo`
5. Click "Generate token"
6. Copy the token and paste it in your `.env` file

## Usage

### Quick Command Script

The easiest way to use this is via the `fetch_github_docs.py` script:

```bash
# Open user stories
python fetch_github_docs.py user-stories

# Open requirements
python fetch_github_docs.py requirements

# Open technical specifications
python fetch_github_docs.py tech-specs

# Open all document types
python fetch_github_docs.py all

# List available documents without opening
python fetch_github_docs.py list
```

### Python API

You can also use it programmatically:

```python
from github_integration import (
    open_user_stories,
    open_requirements,
    open_tech_specs,
    open_all_documents,
    list_available_documents
)

# Open user stories
files = open_user_stories()

# Open requirements
files = open_requirements()

# Open tech specs
files = open_tech_specs()

# Open all documents
files = open_all_documents()

# List without opening
docs = list_available_documents()
for doc_type, file_list in docs.items():
    print(f"{doc_type}: {len(file_list)} files")
```

### Advanced Usage

For more control, use the `GitHubFetcher` class directly:

```python
from github_fetcher import GitHubFetcher

# Initialize with custom settings
fetcher = GitHubFetcher(
    repo_owner="my-org",
    repo_name="my-repo",
    github_token="your_token",
    branch="main"
)

# List all files
files = fetcher.list_files()

# Find specific documents
docs = fetcher.find_documents(["user_stories", "requirements"])

# Download a specific file
local_path = fetcher.download_file("docs/user_stories.md")

# Open file in Cursor
fetcher.open_in_cursor(local_path)
```

### Command-Line Interface

The `github_fetcher.py` module also provides a full CLI:

```bash
# List all files
python github_fetcher.py --list-only

# Fetch and open a specific file
python github_fetcher.py --file "docs/user_stories.md"

# Search for specific document types
python github_fetcher.py --doc-types user_stories requirements

# Use custom repository
python github_fetcher.py \
    --repo-owner my-org \
    --repo-name my-repo \
    --token your_token \
    --doc-types user_stories
```

## How It Works

1. **Authentication**: Uses GitHub Personal Access Token to authenticate API requests
2. **File Discovery**: Searches repository for files matching document type keywords
3. **File Download**: Downloads files from GitHub API and saves them locally
4. **Cursor Integration**: Opens downloaded files in Cursor IDE using the `cursor` command

## Document Detection

The system automatically detects documents by searching for keywords in filenames and paths:

- **User Stories**: `user_stories`, `user_story`
- **Requirements**: `requirements`, `requirement`
- **Technical Specs**: `tech_spec`, `technical_spec`, `technical_specification`

Supported file formats: `.md`, `.txt`, `.json`, `.docx`, `.pdf`

## File Storage

Downloaded files are stored in:
```
<workspace_root>/github_docs/
```

This keeps your GitHub files separate from your local project files while still accessible in Cursor.

## Troubleshooting

### "GitHub token is required" Error

Make sure you've set `GITHUB_TOKEN` in your `.env` file or passed it as a parameter.

### "Repository owner and name are required" Error

Set `GITHUB_REPO_OWNER` and `GITHUB_REPO_NAME` in your `.env` file.

### Files Not Opening in Cursor

1. Make sure Cursor CLI is installed and in your PATH
2. Try using the `code` command (VS Code compatible)
3. Files will open with your system's default handler as a fallback

### No Files Found

- Check that your repository name and owner are correct
- Verify the branch name (defaults to `main`)
- Ensure files exist in the repository
- Check that file names contain the expected keywords

## Examples

### Example 1: Quick Access to User Stories

```bash
# Set environment variables (PowerShell)
$env:GITHUB_TOKEN="ghp_xxxxxxxxxxxx"
$env:GITHUB_REPO_OWNER="my-org"
$env:GITHUB_REPO_NAME="my-project"

# Fetch and open
python fetch_github_docs.py user-stories
```

### Example 2: List All Available Documents

```bash
python fetch_github_docs.py list
```

Output:
```
=== Available Documents ===

USER_STORIES:
  - docs/user_stories.md (15234 bytes)
  - artifacts/user_story_001.md (8234 bytes)

REQUIREMENTS:
  - docs/requirements.md (23456 bytes)

TECH_SPEC:
  - docs/technical_specification.md (45678 bytes)

Total: 4 document(s) found
```

### Example 3: Programmatic Access

```python
from github_integration import open_user_stories

# Opens user stories interactively
opened_files = open_user_stories()
print(f"Opened {len(opened_files)} files")
```

## Integration with Your Workflow

You can integrate this into your existing agents:

```python
# In your agent code
from github_integration import list_available_documents

# Check what documents are available before processing
docs = list_available_documents()
if docs.get("user_stories"):
    print(f"Found {len(docs['user_stories'])} user story files")
```

## Security Notes

- Never commit your `.env` file to version control
- Keep your GitHub token secure
- Use tokens with minimal required permissions
- Consider using GitHub App tokens for production use

## License

Part of the Agentic SDLC Automation Platform.
