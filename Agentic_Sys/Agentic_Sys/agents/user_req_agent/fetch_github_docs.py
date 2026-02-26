#!/usr/bin/env python3
"""
Quick command script to fetch, open, and sync GitHub documents in Cursor.

Usage:
    python fetch_github_docs.py user-stories     # Fetch and open user stories
    python fetch_github_docs.py requirements      # Fetch and open requirements
    python fetch_github_docs.py tech-specs        # Fetch and open tech specs
    python fetch_github_docs.py all               # Fetch and open all documents
    python fetch_github_docs.py list              # List available documents
    python fetch_github_docs.py push              # Push all local changes to GitHub
    python fetch_github_docs.py watch             # Auto-sync: watch for changes & push
"""

import sys
import os
from pathlib import Path

# Add current directory to path
sys.path.insert(0, str(Path(__file__).parent))

from github_integration import (
    open_user_stories,
    open_requirements,
    open_tech_specs,
    open_all_documents,
    list_available_documents
)


def main():
    if len(sys.argv) < 2:
        print("""
GitHub Documents - Two-Way Sync for Cursor IDE

Usage:
    python fetch_github_docs.py <command>

Commands (Fetch from GitHub):
    user-stories    - Fetch and open user stories documents
    requirements    - Fetch and open requirements documents
    tech-specs      - Fetch and open technical specifications
    all             - Fetch and open all document types (matching keywords)
    list            - List documents matching keywords
    list-all        - List ALL files in repository (no keyword filtering)
    download-all    - Download ALL files from repository (no keyword filtering)

Commands (Push to GitHub):
    push            - Push all local changes to GitHub (one-time)
    watch           - Start auto-sync: watches for file changes & pushes to GitHub

Commands (Change Propagation - Bidirectional Sync):
    propagate <file>  - Propagate changes from a document to all related docs
    auto-sync         - Watch for doc changes and auto-propagate + push to GitHub

Commands (Metadata):
    show-metadata   - Show GitHub repository info for all downloaded files
    show-metadata <file> - Show GitHub repository info for a specific file
    update-metadata - Retroactively add metadata for existing files

Environment Variables Required:
    GITHUB_TOKEN        - Your GitHub personal access token
    GITHUB_REPO_OWNER   - GitHub repository owner (username or org)
    GITHUB_REPO_NAME    - Repository name
    GITHUB_BRANCH       - Branch name (optional, defaults to 'main')

Examples:
    python fetch_github_docs.py user-stories     # Open user stories in Cursor
    python fetch_github_docs.py push             # Push edits back to GitHub
    python fetch_github_docs.py watch            # Auto-sync on every save
    python fetch_github_docs.py propagate <file> # Propagate doc change to all related docs
    python fetch_github_docs.py auto-sync        # Watch + propagate + push automatically
        """)
        return 1
    
    command = sys.argv[1].lower()
    
    try:
        if command == "user-stories":
            print("Fetching user stories...")
            files = open_user_stories()
            print(f"[OK] Opened {len(files)} user story file(s) in Cursor")
        
        elif command == "requirements":
            print("Fetching requirements...")
            files = open_requirements()
            print(f"[OK] Opened {len(files)} requirements file(s) in Cursor")
        
        elif command == "tech-specs":
            print("Fetching technical specifications...")
            files = open_tech_specs()
            print(f"[OK] Opened {len(files)} tech spec file(s) in Cursor")
        
        elif command == "all":
            print("Fetching all documents...")
            files = open_all_documents()
            print(f"[OK] Opened {len(files)} file(s) in Cursor")
        
        elif command == "list":
            print("Listing available documents...")
            docs = list_available_documents()
            
            # Display order and labels
            display_labels = {
                "parsed": "PARSED DOCUMENTS (Raw Input)",
                "requirements": "REQUIREMENTS",
                "user_stories": "USER STORIES",
                "tech_spec": "TECHNICAL SPECIFICATIONS",
                "other": "OTHER FILES",
            }
            display_order = ["parsed", "requirements", "user_stories", "tech_spec", "other"]
            
            print("\n=== Available Documents ===")
            total = 0
            for category in display_order:
                file_list = docs.get(category, [])
                if file_list:
                    label = display_labels.get(category, category.upper())
                    print(f"\n{label} ({len(file_list)} files):")
                    for file in file_list:
                        print(f"  - {file['path']} ({file['size']} bytes)")
                        total += 1
            print(f"\nTotal: {total} document(s) found")
        
        elif command == "list-all":
            # List ALL files in the repository, not just ones matching keywords
            from github_fetcher import GitHubFetcher
            fetcher = GitHubFetcher()
            all_files = fetcher.list_files(file_extensions=[".md", ".txt", ".json", ".docx", ".pdf"])
            print(f"\n=== All Files in Repository ===")
            print(f"Total files found: {len(all_files)}")
            for file in all_files:
                print(f"  - {file['path']} ({file['size']} bytes)")
        
        elif command == "download-all":
            # Download ALL files from repository, not just ones matching keywords
            from github_fetcher import GitHubFetcher
            fetcher = GitHubFetcher()
            all_files = fetcher.list_files(file_extensions=[".md", ".txt", ".json", ".docx", ".pdf"])
            
            print(f"\n>> Downloading ALL {len(all_files)} file(s) from repository...")
            downloaded_count = 0
            error_count = 0
            opened_files = []
            
            for i, file in enumerate(all_files, 1):
                try:
                    print(f"  [{i}/{len(all_files)}] Downloading: {file['path']}", end="", flush=True)
                    local_path = fetcher.download_file(file["path"])
                    downloaded_count += 1
                    opened_files.append(local_path)
                    print(f" [OK]", flush=True)
                except Exception as e:
                    error_count += 1
                    print(f" [FAIL] Error: {e}", flush=True)
            
            print(f"\n{'='*60}")
            print(f"  Download Summary:")
            print(f"  [OK] Successfully downloaded: {downloaded_count} file(s)")
            if error_count > 0:
                print(f"  [FAIL] Failed to download: {error_count} file(s)")
            print(f"  >> Location: {fetcher.github_docs_root}")
            print(f"{'='*60}")
        
        elif command == "push":
            from github_sync_watcher import GitHubSyncWatcher
            print("Pushing local changes to GitHub...")
            watcher = GitHubSyncWatcher()
            count = watcher.push_all_changes()
            if count > 0:
                print(f"\n[OK] Pushed {count} file(s) to GitHub")
            else:
                print("\n[OK] Everything is up to date")
        
        elif command == "watch":
            from github_sync_watcher import GitHubSyncWatcher
            watcher = GitHubSyncWatcher()
            watcher.watch()
        
        elif command == "show-metadata":
            from github_fetcher import GitHubFetcher
            import json
            fetcher = GitHubFetcher()
            
            if len(sys.argv) > 2:
                # Show metadata for specific file
                file_path = sys.argv[2]
                metadata = fetcher.get_file_metadata(file_path)
                if metadata:
                    print(f"\n=== GitHub Repository Info for: {file_path} ===")
                    print(f"Repository: {metadata['repo_owner']}/{metadata['repo_name']}")
                    print(f"Repository URL: {metadata['repo_url']}")
                    print(f"Branch: {metadata['branch']}")
                    print(f"File Path in Repo: {metadata['repo_path']}")
                    print(f"SHA: {metadata['sha']}")
                    print(f"Downloaded At: {metadata['downloaded_at']}")
                else:
                    print(f"No metadata found for: {file_path}")
                    print("This file may not have been downloaded via the GitHub integration.")
            else:
                # Show metadata for all files
                metadata = fetcher._load_metadata()
                if metadata:
                    print(f"\n=== GitHub Repository Tracking ({len(metadata)} file(s)) ===\n")
                    
                    # Group by repository
                    repos = {}
                    for file_path, meta in metadata.items():
                        repo_key = f"{meta['repo_owner']}/{meta['repo_name']}"
                        if repo_key not in repos:
                            repos[repo_key] = {
                                'url': meta['repo_url'],
                                'branch': meta['branch'],
                                'files': []
                            }
                        repos[repo_key]['files'].append({
                            'local_path': file_path,
                            'repo_path': meta['repo_path'],
                            'downloaded_at': meta['downloaded_at']
                        })
                    
                    for repo_key, repo_info in repos.items():
                        print(f"Repository: {repo_key}")
                        print(f"  URL: {repo_info['url']}")
                        print(f"  Branch: {repo_info['branch']}")
                        print(f"  Files ({len(repo_info['files'])}):")
                        for file_info in sorted(repo_info['files'], key=lambda x: x['local_path']):
                            print(f"    - {file_info['local_path']}")
                            print(f"      Repo Path: {file_info['repo_path']}")
                            print(f"      Downloaded: {file_info['downloaded_at']}")
                        print()
                else:
                    print("No metadata found. Files may not have been downloaded via the GitHub integration.")
                    print("Run 'update-metadata' to add metadata for existing files.")
        
        elif command == "update-metadata":
            from github_fetcher import GitHubFetcher
            print("Updating metadata for existing files...")
            fetcher = GitHubFetcher()
            updated_count = fetcher.update_metadata_for_existing_files()
            if updated_count > 0:
                print(f"[OK] Updated metadata for {updated_count} file(s)")
                print("Run 'show-metadata' to view the repository information.")
            else:
                print("[OK] All files already have metadata, or no files found.")
        
        elif command == "propagate":
            from change_propagation import ChangePropagationEngine
            
            if len(sys.argv) < 3:
                print("Error: Please provide the file path to propagate.")
                print("Usage: python fetch_github_docs.py propagate <file_path>")
                print("")
                print("Examples:")
                print("  python fetch_github_docs.py propagate github_docs/requirements/2ef703ae/Avni_SmartGovProj_Req_1_requirements.md")
                print("  python fetch_github_docs.py propagate C:/Users/.../Avni_SmartGovProj_Req_1_requirements.md")
                print("")
                print("Propagation rules:")
                print("  *_requirements.md  -> Re-generates user stories + tech specs")
                print("  *_user_stories.md  -> Re-generates tech specs")
                print("  *_tech_spec.md     -> Re-generates user stories")
                print("  *_parsed.md        -> Full pipeline: requirements + stories + specs")
                return 1
            
            file_path = sys.argv[2]
            
            # Resolve relative paths
            if not os.path.isabs(file_path):
                # Try from workspace root (github_docs/)
                from github_fetcher import GitHubFetcher
                fetcher = GitHubFetcher()
                candidate = fetcher.github_docs_root / file_path
                if candidate.exists():
                    file_path = str(candidate)
                elif not Path(file_path).exists():
                    # Try from github_docs root
                    candidate = fetcher.github_docs_root / file_path
                    if not candidate.exists():
                        print(f"Error: File not found: {file_path}")
                        return 1
                    file_path = str(candidate)
            
            if not Path(file_path).exists():
                print(f"Error: File not found: {file_path}")
                return 1
            
            engine = ChangePropagationEngine()
            results = engine.propagate_change(file_path)
            
            print(f"\n{'='*60}")
            print(f"  Propagation Complete:")
            for doc_type, path in results.items():
                print(f"  [OK] {doc_type}: {Path(path).name}")
            print(f"{'='*60}")
        
        elif command == "auto-sync":
            from change_propagation import ChangePropagationEngine
            
            debounce = 5.0
            if len(sys.argv) > 2:
                try:
                    debounce = float(sys.argv[2])
                except ValueError:
                    pass
            
            engine = ChangePropagationEngine()
            engine.watch_and_propagate(debounce_seconds=debounce)
        
        else:
            print(f"Unknown command: {command}")
            print("Commands: user-stories, requirements, tech-specs, all, list, list-all,")
            print("          download-all, push, watch, propagate, auto-sync,")
            print("          show-metadata, update-metadata")
            return 1
        
        return 0
    
    except ValueError as e:
        print(f"Configuration Error: {e}")
        print("\nPlease set the following environment variables:")
        print("  - GITHUB_TOKEN (required)")
        print("  - GITHUB_REPO_OWNER (required)")
        print("  - GITHUB_REPO_NAME (required)")
        print("  - GITHUB_BRANCH (optional, defaults to 'main')")
        return 1
    
    except Exception as e:
        print(f"Error: {e}")
        return 1


if __name__ == "__main__":
    exit(main())
