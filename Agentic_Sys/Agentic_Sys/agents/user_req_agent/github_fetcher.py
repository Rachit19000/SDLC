"""
GitHub File Fetcher - Two-way sync between GitHub repository and Cursor IDE.

This module provides utilities to:
- Fetch files from GitHub repositories
- List available documents (user stories, requirements, tech specs)
- Download files locally for opening in Cursor
- Open files directly in Cursor IDE
- Push local changes back to GitHub (two-way sync)
"""

import os
import json
import base64
import subprocess
import time
from pathlib import Path
from typing import List, Dict, Optional
from urllib.parse import urlparse
from datetime import datetime
import requests
from dotenv import load_dotenv

# Load .env file - prefer common one in agents/ folder, fallback to local
def find_and_load_env():
    """Find and load .env file from agents/ folder (common) or current directory."""
    current_dir = Path(__file__).parent
    
    # First, try common .env in agents/ folder (parent directory)
    agents_env = current_dir.parent / ".env"
    if agents_env.exists():
        load_dotenv(agents_env)
        return
    
    # Fallback: try current directory
    local_env = current_dir / ".env"
    if local_env.exists():
        load_dotenv(local_env)
        return
    
    # Last resort: default behavior (current working directory)
    load_dotenv()

find_and_load_env()


class GitHubFetcher:
    """Fetches files from GitHub repository using GitHub API."""
    
    def __init__(
        self,
        repo_owner: Optional[str] = None,
        repo_name: Optional[str] = None,
        github_token: Optional[str] = None,
        branch: str = "main"
    ):
        """
        Initialize GitHub fetcher.
        
        Args:
            repo_owner: GitHub repository owner (username or org)
            repo_name: Repository name
            github_token: GitHub personal access token (or from GITHUB_TOKEN env var)
            branch: Branch name (default: main)
        """
        self.repo_owner = repo_owner or os.getenv("GITHUB_REPO_OWNER")
        self.repo_name = repo_name or os.getenv("GITHUB_REPO_NAME")
        self.github_token = github_token or os.getenv("GITHUB_TOKEN")
        self.branch = branch or os.getenv("GITHUB_BRANCH", "main")
        
        if not self.github_token:
            raise ValueError(
                "GitHub token is required. Set GITHUB_TOKEN environment variable "
                "or pass github_token parameter."
            )
        
        if not self.repo_owner or not self.repo_name:
            raise ValueError(
                "Repository owner and name are required. Set GITHUB_REPO_OWNER "
                "and GITHUB_REPO_NAME environment variables or pass as parameters."
            )
        
        self.base_url = f"https://api.github.com/repos/{self.repo_owner}/{self.repo_name}"
        self.headers = {
            "Authorization": f"token {self.github_token}",
            "Accept": "application/vnd.github.v3+json",
            "User-Agent": "Cursor-GitHub-Integration"
        }
    
    @property
    def github_docs_root(self) -> Path:
        """Return the local github_docs directory path."""
        workspace_root = Path(__file__).parent.parent.parent.parent.parent
        return workspace_root / "github_docs"
    
    @property
    def metadata_file(self) -> Path:
        """Return the path to the metadata JSON file."""
        return self.github_docs_root / ".github_metadata.json"
    
    def _load_metadata(self) -> Dict:
        """Load metadata from JSON file."""
        if self.metadata_file.exists():
            try:
                with open(self.metadata_file, "r", encoding="utf-8") as f:
                    return json.load(f)
            except (json.JSONDecodeError, IOError):
                return {}
        return {}
    
    def _save_metadata(self, metadata: Dict):
        """Save metadata to JSON file."""
        self.github_docs_root.mkdir(parents=True, exist_ok=True)
        with open(self.metadata_file, "w", encoding="utf-8") as f:
            json.dump(metadata, f, indent=2, ensure_ascii=False)
    
    def _update_file_metadata(self, local_path: Path, repo_path: str, file_sha: str):
        """Update metadata for a downloaded file."""
        metadata = self._load_metadata()
        
        # Convert local path to relative path from github_docs_root
        try:
            relative_path = str(local_path.relative_to(self.github_docs_root)).replace("\\", "/")
        except ValueError:
            relative_path = str(local_path)
        
        metadata[relative_path] = {
            "repo_owner": self.repo_owner,
            "repo_name": self.repo_name,
            "repo_url": f"https://github.com/{self.repo_owner}/{self.repo_name}",
            "branch": self.branch,
            "repo_path": repo_path,
            "sha": file_sha,
            "downloaded_at": datetime.now().isoformat(),
            "local_path": str(local_path)
        }
        
        self._save_metadata(metadata)
    
    def get_file_metadata(self, local_path: str) -> Optional[Dict]:
        """
        Get metadata for a local file.
        
        Args:
            local_path: Path to local file
            
        Returns:
            Metadata dictionary or None if not found
        """
        local_path = Path(local_path)
        metadata = self._load_metadata()
        
        # Try absolute path first
        try:
            relative_path = str(local_path.relative_to(self.github_docs_root)).replace("\\", "/")
            return metadata.get(relative_path)
        except ValueError:
            # Try just the filename
            filename = local_path.name
            for path, meta in metadata.items():
                if Path(path).name == filename:
                    return meta
        return None
    
    def update_metadata_for_existing_files(self) -> int:
        """
        Retroactively update metadata for existing files in github_docs.
        Matches local files with GitHub repository files.
        
        Returns:
            Number of files updated
        """
        if not self.github_docs_root.exists():
            return 0
        
        # Get all files from GitHub
        all_github_files = self.list_files(file_extensions=[".md", ".txt", ".json", ".docx", ".pdf"])
        github_file_map = {file["path"]: file for file in all_github_files}
        
        # Get all local files
        local_files = list(self.github_docs_root.rglob("*"))
        local_files = [f for f in local_files if f.is_file()]
        
        updated_count = 0
        metadata = self._load_metadata()
        
        for local_file in local_files:
            try:
                relative_path = str(local_file.relative_to(self.github_docs_root)).replace("\\", "/")
                
                # Skip if already has metadata
                if relative_path in metadata:
                    continue
                
                # Try to match with GitHub file
                if relative_path in github_file_map:
                    github_file = github_file_map[relative_path]
                    self._update_file_metadata(local_file, relative_path, github_file.get("sha", ""))
                    updated_count += 1
            except Exception:
                # Skip files that can't be processed
                continue
        
        return updated_count
    
    def _make_request(self, endpoint: str, method: str = "GET", json_data: Dict = None) -> Dict:
        """Make a request to GitHub API."""
        url = f"{self.base_url}/{endpoint}"
        if method == "GET":
            response = requests.get(url, headers=self.headers)
        elif method == "PUT":
            response = requests.put(url, headers=self.headers, json=json_data)
        elif method == "POST":
            response = requests.post(url, headers=self.headers, json=json_data)
        elif method == "DELETE":
            response = requests.delete(url, headers=self.headers, json=json_data)
        else:
            raise ValueError(f"Unsupported HTTP method: {method}")
        response.raise_for_status()
        return response.json()
    
    def list_files(
        self,
        path: str = "",
        file_extensions: Optional[List[str]] = None
    ) -> List[Dict]:
        """
        List files in the repository.
        
        Args:
            path: Directory path in repository (empty for root)
            file_extensions: Filter by file extensions (e.g., ['.md', '.txt', '.json'])
        
        Returns:
            List of file information dictionaries
        """
        endpoint = f"contents/{path}" if path else "contents"
        if self.branch:
            endpoint += f"?ref={self.branch}"
        
        try:
            contents = self._make_request(endpoint)
            files = []
            
            for item in contents:
                if item["type"] == "file":
                    if file_extensions:
                        file_ext = Path(item["name"]).suffix.lower()
                        if file_ext in file_extensions:
                            files.append({
                                "name": item["name"],
                                "path": item["path"],
                                "size": item["size"],
                                "url": item["download_url"],
                                "sha": item["sha"]
                            })
                    else:
                        files.append({
                            "name": item["name"],
                            "path": item["path"],
                            "size": item["size"],
                            "url": item["download_url"],
                            "sha": item["sha"]
                        })
                elif item["type"] == "dir":
                    # Recursively get files from subdirectories
                    subfiles = self.list_files(item["path"], file_extensions)
                    files.extend(subfiles)
            
            return files
        except requests.exceptions.HTTPError as e:
            if e.response.status_code == 404:
                return []
            raise
    
    def find_documents(
        self,
        doc_types: Optional[List[str]] = None
    ) -> Dict[str, List[Dict]]:
        """
        Find and categorize documents by type based on FILENAME patterns.
        
        Categories:
          - parsed: files ending with _parsed.md
          - requirements: files ending with _requirements.md
          - user_stories: files ending with _user_stories.md
          - tech_spec: files ending with _tech_spec.md or _tech_specs.md
          - other: files that don't match any pattern
        
        Args:
            doc_types: Ignored (kept for backward compat). Categories are auto-detected.
        
        Returns:
            Dictionary mapping document categories to lists of matching files
        """
        all_files = self.list_files(file_extensions=[".md", ".txt", ".json", ".docx", ".pdf"])
        
        # Define categories by FILENAME suffix patterns (not folder path)
        categories = {
            "parsed": ["_parsed.md", "_parsed.txt"],
            "requirements": ["_requirements.md", "_requirements.txt"],
            "user_stories": ["_user_stories.md", "_user_stories.txt", "_user_story.md"],
            "tech_spec": ["_tech_spec.md", "_tech_specs.md", "_tech_spec.txt"],
        }
        
        documents = {cat: [] for cat in categories}
        documents["other"] = []
        
        for file in all_files:
            file_name_lower = file["name"].lower()
            categorized = False
            
            for category, suffixes in categories.items():
                for suffix in suffixes:
                    if file_name_lower.endswith(suffix):
                        documents[category].append(file)
                        categorized = True
                        break
                if categorized:
                    break
            
            if not categorized:
                documents["other"].append(file)
        
        return documents
    
    def get_file_content(self, file_path: str) -> str:
        """
        Get content of a file from GitHub.
        
        Args:
            file_path: Path to file in repository
        
        Returns:
            File content as string
        """
        endpoint = f"contents/{file_path}"
        if self.branch:
            endpoint += f"?ref={self.branch}"
        
        file_info = self._make_request(endpoint)
        
        if file_info["encoding"] == "base64":
            content = base64.b64decode(file_info["content"]).decode("utf-8")
        else:
            content = file_info["content"]
        
        return content
    
    def download_file(
        self,
        file_path: str,
        local_path: Optional[str] = None,
        create_dirs: bool = True
    ) -> str:
        """
        Download a file from GitHub and save it locally.
        
        Args:
            file_path: Path to file in repository
            local_path: Local path to save file (default: creates in github_docs/ directory)
            create_dirs: Create directories if they don't exist
        
        Returns:
            Path to downloaded file
        """
        if local_path is None:
            # Create a github_docs directory in the workspace
            workspace_root = Path(__file__).parent.parent.parent.parent.parent
            local_path = workspace_root / "github_docs" / file_path
        
        local_path = Path(local_path)
        
        if create_dirs:
            local_path.parent.mkdir(parents=True, exist_ok=True)
        
        # Get file info first to retrieve SHA for metadata
        endpoint = f"contents/{file_path}"
        if self.branch:
            endpoint += f"?ref={self.branch}"
        
        file_info = self._make_request(endpoint)
        file_sha = file_info.get("sha", "")
        
        # Check if file is binary (based on extension)
        binary_extensions = {'.pdf', '.docx', '.doc', '.xlsx', '.xls', '.pptx', '.ppt', '.zip', '.exe', '.dll', '.png', '.jpg', '.jpeg', '.gif'}
        is_binary = Path(file_path).suffix.lower() in binary_extensions
        
        if is_binary:
            # For binary files, download directly from download_url
            download_url = file_info.get("download_url")
            
            if download_url:
                response = requests.get(download_url, headers=self.headers)
                response.raise_for_status()
                with open(local_path, "wb") as f:
                    f.write(response.content)
            else:
                # Fallback: try to decode base64
                if file_info["encoding"] == "base64":
                    content_bytes = base64.b64decode(file_info["content"])
                    with open(local_path, "wb") as f:
                        f.write(content_bytes)
                else:
                    raise ValueError(f"Cannot download binary file: {file_path}")
        else:
            # For text files, decode from base64
            if file_info["encoding"] == "base64":
                content = base64.b64decode(file_info["content"]).decode("utf-8")
            else:
                content = file_info["content"]
            
            with open(local_path, "w", encoding="utf-8") as f:
                f.write(content)
        
        # Save metadata after successful download
        self._update_file_metadata(local_path, file_path, file_sha)
        
        return str(local_path)
    
    def get_file_sha(self, file_path: str) -> Optional[str]:
        """
        Get the SHA of a file in the GitHub repository.
        Required for updating existing files via the API.
        
        Args:
            file_path: Path to file in repository
        
        Returns:
            SHA string, or None if file doesn't exist
        """
        endpoint = f"contents/{file_path}"
        if self.branch:
            endpoint += f"?ref={self.branch}"
        try:
            file_info = self._make_request(endpoint)
            return file_info.get("sha")
        except requests.exceptions.HTTPError as e:
            if e.response.status_code == 404:
                return None
            raise
    
    def push_file(
        self,
        local_path: str,
        repo_path: Optional[str] = None,
        commit_message: Optional[str] = None
    ) -> Dict:
        """
        Push a local file to GitHub repository.
        
        Args:
            local_path: Path to local file
            repo_path: Path in the repository (auto-detected from github_docs structure if None)
            commit_message: Commit message (auto-generated if None)
        
        Returns:
            GitHub API response with commit info
        """
        local_path = Path(local_path)
        
        if not local_path.exists():
            raise FileNotFoundError(f"Local file not found: {local_path}")
        
        # Auto-detect repo path from github_docs structure
        if repo_path is None:
            github_docs_dir = self.github_docs_root
            try:
                repo_path = str(local_path.relative_to(github_docs_dir)).replace("\\", "/")
            except ValueError:
                raise ValueError(
                    f"Cannot determine repo path. File {local_path} is not inside "
                    f"{github_docs_dir}. Please provide repo_path explicitly."
                )
        
        # Read local file content
        with open(local_path, "r", encoding="utf-8") as f:
            content = f.read()
        
        # Encode content to base64
        content_b64 = base64.b64encode(content.encode("utf-8")).decode("utf-8")
        
        # Generate commit message
        if commit_message is None:
            commit_message = f"Update {repo_path} via Cursor IDE"
        
        # Check if file exists to get its SHA (required for updates)
        sha = self.get_file_sha(repo_path)
        
        # Build request payload
        payload = {
            "message": commit_message,
            "content": content_b64,
            "branch": self.branch
        }
        if sha:
            payload["sha"] = sha
        
        # Push to GitHub
        endpoint = f"contents/{repo_path}"
        result = self._make_request(endpoint, method="PUT", json_data=payload)
        
        return {
            "status": "updated" if sha else "created",
            "path": repo_path,
            "sha": result["content"]["sha"],
            "commit_sha": result["commit"]["sha"],
            "commit_url": result["commit"]["html_url"]
        }
    
    def push_all_changes(self, commit_message: Optional[str] = None) -> List[Dict]:
        """
        Push all modified local files back to GitHub.
        Compares local files with GitHub versions and pushes changes.
        
        Args:
            commit_message: Commit message prefix
        
        Returns:
            List of push results
        """
        github_docs_dir = self.github_docs_root
        
        if not github_docs_dir.exists():
            print("No github_docs directory found. Nothing to push.")
            return []
        
        results = []
        
        # Find all local files
        for local_file in github_docs_dir.rglob("*"):
            if local_file.is_file():
                repo_path = str(local_file.relative_to(github_docs_dir)).replace("\\", "/")
                
                # Read local content
                try:
                    with open(local_file, "r", encoding="utf-8") as f:
                        local_content = f.read()
                except UnicodeDecodeError:
                    print(f"  Skipping binary file: {repo_path}")
                    continue
                
                # Get GitHub content to compare
                try:
                    github_content = self.get_file_content(repo_path)
                except Exception:
                    github_content = None
                
                # Push if content is different or file is new
                if github_content is None or local_content != github_content:
                    msg = commit_message or f"Update {repo_path} via Cursor IDE"
                    try:
                        result = self.push_file(str(local_file), repo_path, msg)
                        results.append(result)
                        status = result["status"]
                        print(f"  [OK] {status.capitalize()}: {repo_path}")
                    except Exception as e:
                        print(f"  [FAIL] Error pushing {repo_path}: {e}")
                else:
                    print(f"  - No changes: {repo_path}")
        
        return results
    
    def open_in_cursor(self, file_path: str) -> bool:
        """
        Open a file in Cursor IDE.
        
        Args:
            file_path: Path to file (can be local or GitHub path)
        
        Returns:
            True if successful, False otherwise
        """
        # If it's a GitHub path, download it first
        if not Path(file_path).exists():
            try:
                file_path = self.download_file(file_path)
            except Exception as e:
                print(f"Error downloading file: {e}")
                return False
        
        # Open file in Cursor using the 'cursor' command (non-blocking)
        try:
            # Try using 'cursor' command (if Cursor CLI is installed)
            subprocess.Popen(["cursor", file_path])
            return True
        except (OSError, FileNotFoundError):
            try:
                # Fallback: try 'code' command (VS Code compatible)
                subprocess.Popen(["code", file_path])
                return True
            except (OSError, FileNotFoundError):
                # Last resort: open with default system handler
                try:
                    if os.name == "nt":  # Windows
                        os.startfile(file_path)
                    elif os.name == "posix":  # macOS/Linux
                        subprocess.run(["open" if os.uname().sysname == "Darwin" else "xdg-open", file_path])
                    return True
                except Exception as e:
                    print(f"Error opening file: {e}")
                    return False
    
    def list_and_open_documents(
        self,
        doc_types: Optional[List[str]] = None,
        interactive: bool = True
    ) -> List[str]:
        """
        List available documents and optionally open them.
        
        Args:
            doc_types: Document types to search for
            interactive: If True, prompt user to select files to open
        
        Returns:
            List of opened file paths
        """
        documents = self.find_documents(doc_types)
        opened_files = []
        
        print("\n=== Available Documents ===")
        for doc_type, files in documents.items():
            if files:
                print(f"\n{doc_type.upper()}:")
                for i, file in enumerate(files, 1):
                    print(f"  {i}. {file['path']} ({file['size']} bytes)")
        
        if not interactive:
            # Open all found documents (deduplicate first)
            unique_files = {}
            for doc_type, files in documents.items():
                for file in files:
                    file_path = file["path"]
                    if file_path not in unique_files:
                        unique_files[file_path] = file
            
            print(f"\n>> Downloading {len(unique_files)} unique file(s)...")
            for i, (file_path, file) in enumerate(unique_files.items(), 1):
                try:
                    print(f"  [{i}/{len(unique_files)}] Downloading: {file_path}")
                    local_path = self.download_file(file_path)
                    if self.open_in_cursor(local_path):
                        opened_files.append(local_path)
                except Exception as e:
                    print(f"  [FAIL] Error downloading {file_path}: {e}")
            print(f"\n[OK] Downloaded {len(opened_files)} file(s) successfully")
            return opened_files
        
        # Interactive mode
        print("\nEnter file numbers to open (comma-separated, or 'all' for all files):")
        user_input = input("> ").strip()
        
        if user_input.lower() == "all":
            # Deduplicate files by path (same file might appear in multiple categories)
            unique_files = {}
            for doc_type, files in documents.items():
                for file in files:
                    file_path = file["path"]
                    if file_path not in unique_files:
                        unique_files[file_path] = file
            
            print(f"\n>> Downloading {len(unique_files)} unique file(s)...")
            downloaded_count = 0
            error_count = 0
            downloaded_files = []
            
            # First, download all files
            for i, (file_path, file) in enumerate(unique_files.items(), 1):
                try:
                    print(f"  [{i}/{len(unique_files)}] Downloading: {file_path}", end="", flush=True)
                    local_path = self.download_file(file_path)
                    downloaded_count += 1
                    downloaded_files.append(local_path)
                    print(f" [OK]", flush=True)
                except KeyboardInterrupt:
                    print(f"\n  [INTERRUPTED] Download stopped by user")
                    break
                except Exception as e:
                    error_count += 1
                    print(f" [FAIL] Error: {e}", flush=True)
                    # Continue with next file even if this one failed
                    continue
            
            # Skip opening files when downloading "all" - files are already downloaded
            # User can open them manually in Cursor if needed
            print(f"\n{'='*60}")
            print(f"  Download Summary:")
            print(f"  [OK] Successfully downloaded: {downloaded_count} file(s)")
            if error_count > 0:
                print(f"  [FAIL] Failed to download: {error_count} file(s)")
            print(f"  >> Location: {self.github_docs_root}")
            print(f"  >> All files are ready. Open them manually in Cursor if needed.")
            print(f"{'='*60}")
        else:
            try:
                indices = [int(x.strip()) - 1 for x in user_input.split(",")]
                all_files = []
                for files in documents.values():
                    all_files.extend(files)
                
                for idx in indices:
                    if 0 <= idx < len(all_files):
                        file = all_files[idx]
                        local_path = self.download_file(file["path"])
                        if self.open_in_cursor(local_path):
                            opened_files.append(local_path)
            except ValueError:
                print("Invalid input. Please enter numbers separated by commas.")
        
        return opened_files


def main():
    """Command-line interface for GitHub file fetcher."""
    import argparse
    
    parser = argparse.ArgumentParser(
        description="Fetch and open files from GitHub repository in Cursor IDE"
    )
    parser.add_argument(
        "--repo-owner",
        help="GitHub repository owner (username or org)"
    )
    parser.add_argument(
        "--repo-name",
        help="GitHub repository name"
    )
    parser.add_argument(
        "--token",
        help="GitHub personal access token"
    )
    parser.add_argument(
        "--branch",
        default="main",
        help="Branch name (default: main)"
    )
    parser.add_argument(
        "--path",
        default="",
        help="Directory path in repository (default: root)"
    )
    parser.add_argument(
        "--file",
        help="Specific file path to fetch and open"
    )
    parser.add_argument(
        "--doc-types",
        nargs="+",
        help="Document types to search for (e.g., user_stories requirements tech_spec)"
    )
    parser.add_argument(
        "--list-only",
        action="store_true",
        help="Only list files, don't open them"
    )
    parser.add_argument(
        "--download-dir",
        help="Directory to download files to (default: github_docs/)"
    )
    
    args = parser.parse_args()
    
    try:
        fetcher = GitHubFetcher(
            repo_owner=args.repo_owner,
            repo_name=args.repo_name,
            github_token=args.token,
            branch=args.branch
        )
        
        if args.file:
            # Fetch and open specific file
            print(f"Fetching file: {args.file}")
            local_path = fetcher.download_file(args.file, args.download_dir)
            print(f"Downloaded to: {local_path}")
            if not args.list_only:
                fetcher.open_in_cursor(local_path)
        elif args.doc_types:
            # Find and open documents by type
            opened = fetcher.list_and_open_documents(
                doc_types=args.doc_types,
                interactive=not args.list_only
            )
            if opened:
                print(f"\nOpened {len(opened)} file(s) in Cursor")
        else:
            # List all files
            files = fetcher.list_files(path=args.path)
            print(f"\nFound {len(files)} file(s):")
            for file in files:
                print(f"  - {file['path']}")
            
            if not args.list_only and files:
                print("\nUse --file or --doc-types to fetch specific files")
    
    except Exception as e:
        print(f"Error: {e}")
        return 1
    
    return 0


if __name__ == "__main__":
    exit(main())
