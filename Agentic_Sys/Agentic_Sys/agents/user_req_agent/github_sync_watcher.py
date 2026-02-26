"""
GitHub Sync Watcher - Watches for local file changes and auto-pushes to GitHub.

This module monitors the github_docs/ directory for file changes made in Cursor
and automatically pushes them to the GitHub repository (two-way sync).

Usage:
    python github_sync_watcher.py          # Start watching for changes
    python github_sync_watcher.py --once   # Push all changes once and exit
"""

import os
import sys
import time
import hashlib
from pathlib import Path
from typing import Dict, Optional
from dotenv import load_dotenv

load_dotenv()

from github_fetcher import GitHubFetcher


class GitHubSyncWatcher:
    """Watches github_docs/ for changes and syncs to GitHub."""
    
    def __init__(self, fetcher: Optional[GitHubFetcher] = None, debounce_seconds: float = 3.0):
        """
        Initialize the sync watcher.
        
        Args:
            fetcher: GitHubFetcher instance (creates one from env if None)
            debounce_seconds: Wait time after last change before pushing (default: 3s)
        """
        self.fetcher = fetcher or GitHubFetcher()
        self.debounce_seconds = debounce_seconds
        self.github_docs_dir = self.fetcher.github_docs_root
        self._file_hashes: Dict[str, str] = {}
        self._pending_changes: Dict[str, float] = {}  # path -> last_modified_time
    
    def _get_file_hash(self, file_path: Path) -> Optional[str]:
        """Get MD5 hash of a file's content."""
        try:
            with open(file_path, "rb") as f:
                return hashlib.md5(f.read()).hexdigest()
        except (OSError, IOError):
            return None
    
    def _scan_files(self) -> Dict[str, str]:
        """Scan all files in github_docs and return their hashes."""
        file_hashes = {}
        if not self.github_docs_dir.exists():
            return file_hashes
        
        for file_path in self.github_docs_dir.rglob("*"):
            if file_path.is_file():
                file_hash = self._get_file_hash(file_path)
                if file_hash:
                    file_hashes[str(file_path)] = file_hash
        
        return file_hashes
    
    def _detect_changes(self) -> list:
        """Detect files that have changed since last scan."""
        current_hashes = self._scan_files()
        changed_files = []
        
        for file_path, file_hash in current_hashes.items():
            old_hash = self._file_hashes.get(file_path)
            if old_hash is None or old_hash != file_hash:
                changed_files.append(file_path)
        
        # Update stored hashes
        self._file_hashes = current_hashes
        
        return changed_files
    
    def push_single_file(self, local_path: str) -> bool:
        """
        Push a single changed file to GitHub.
        
        Args:
            local_path: Path to the local file
        
        Returns:
            True if successful
        """
        local_path = Path(local_path)
        
        try:
            repo_path = str(local_path.relative_to(self.github_docs_dir)).replace("\\", "/")
        except ValueError:
            print(f"  [FAIL] File not in github_docs: {local_path}")
            return False
        
        try:
            result = self.fetcher.push_file(
                str(local_path),
                repo_path,
                f"Update {repo_path} via Cursor IDE (auto-sync)"
            )
            status = result["status"]
            print(f"  [OK] {status.capitalize()}: {repo_path}")
            return True
        except Exception as e:
            print(f"  [FAIL] Error pushing {repo_path}: {e}")
            return False
    
    def push_all_changes(self) -> int:
        """
        Push all modified files to GitHub.
        
        Returns:
            Number of files pushed
        """
        print("\n>> Checking for changes...")
        results = self.fetcher.push_all_changes()
        count = len(results)
        if count > 0:
            print(f"\n[OK] Pushed {count} file(s) to GitHub")
        else:
            print("\n[OK] No changes to push")
        return count
    
    def watch(self):
        """
        Start watching for file changes and auto-push to GitHub.
        Runs indefinitely until Ctrl+C is pressed.
        """
        print("=" * 60)
        print("  GitHub Two-Way Sync Watcher")
        print("=" * 60)
        print(f"\n  Watching:  {self.github_docs_dir}")
        print(f"  Repo:      {self.fetcher.repo_owner}/{self.fetcher.repo_name}")
        print(f"  Branch:    {self.fetcher.branch}")
        print(f"  Debounce:  {self.debounce_seconds}s")
        print(f"\n  Edit files in Cursor → Changes auto-push to GitHub")
        print(f"  Press Ctrl+C to stop\n")
        print("-" * 60)
        
        # Initial scan to build baseline hashes
        self._file_hashes = self._scan_files()
        file_count = len(self._file_hashes)
        print(f"  >> Tracking {file_count} file(s)")
        print("-" * 60)
        
        try:
            while True:
                # Detect changes
                changed_files = self._detect_changes()
                
                if changed_files:
                    # Record pending changes with timestamp
                    now = time.time()
                    for f in changed_files:
                        self._pending_changes[f] = now
                
                # Process pending changes that have passed the debounce period
                now = time.time()
                ready_files = [
                    f for f, t in self._pending_changes.items()
                    if now - t >= self.debounce_seconds
                ]
                
                if ready_files:
                    timestamp = time.strftime("%H:%M:%S")
                    print(f"\n[{timestamp}] Detected {len(ready_files)} changed file(s):")
                    
                    for file_path in ready_files:
                        self.push_single_file(file_path)
                        del self._pending_changes[file_path]
                    
                    print(f"[{timestamp}] Sync complete [OK]")
                
                # Poll interval
                time.sleep(1)
        
        except KeyboardInterrupt:
            print("\n\n>> Watcher stopped.")
            
            # Push any remaining pending changes
            if self._pending_changes:
                print(f"  Pushing {len(self._pending_changes)} remaining change(s)...")
                for file_path in list(self._pending_changes.keys()):
                    self.push_single_file(file_path)
                print("  Done [OK]")


def main():
    import argparse
    
    parser = argparse.ArgumentParser(
        description="Watch for local file changes and sync to GitHub"
    )
    parser.add_argument(
        "--once",
        action="store_true",
        help="Push all changes once and exit (no watching)"
    )
    parser.add_argument(
        "--debounce",
        type=float,
        default=3.0,
        help="Seconds to wait after last change before pushing (default: 3)"
    )
    parser.add_argument(
        "--file",
        help="Push a specific file to GitHub"
    )
    
    args = parser.parse_args()
    
    try:
        watcher = GitHubSyncWatcher(debounce_seconds=args.debounce)
        
        if args.file:
            # Push a specific file
            print(f"Pushing file: {args.file}")
            watcher.push_single_file(args.file)
        elif args.once:
            # Push all changes once
            watcher.push_all_changes()
        else:
            # Start watching
            watcher.watch()
    
    except ValueError as e:
        print(f"Configuration Error: {e}")
        print("\nMake sure your .env file has GITHUB_TOKEN, GITHUB_REPO_OWNER, GITHUB_REPO_NAME")
        return 1
    except Exception as e:
        print(f"Error: {e}")
        return 1
    
    return 0


if __name__ == "__main__":
    exit(main())
