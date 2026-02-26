"""
GitHub Integration Helper - Quick access functions for fetching documents.

This module provides convenient functions to quickly fetch and open
user stories, requirements, and technical specifications from GitHub.
"""

import os
from typing import List, Optional
from pathlib import Path
from dotenv import load_dotenv
from github_fetcher import GitHubFetcher

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


def get_github_fetcher() -> GitHubFetcher:
    """Get a configured GitHubFetcher instance from environment variables."""
    return GitHubFetcher()


def open_user_stories(repo_owner: Optional[str] = None, repo_name: Optional[str] = None) -> List[str]:
    """
    Fetch and open user stories from GitHub repository.
    
    Args:
        repo_owner: GitHub repository owner (optional, uses env var if not provided)
        repo_name: Repository name (optional, uses env var if not provided)
    
    Returns:
        List of opened file paths
    """
    fetcher = GitHubFetcher(repo_owner=repo_owner, repo_name=repo_name)
    return fetcher.list_and_open_documents(doc_types=["user_stories", "user_story"])


def open_requirements(repo_owner: Optional[str] = None, repo_name: Optional[str] = None) -> List[str]:
    """
    Fetch and open requirements documents from GitHub repository.
    
    Args:
        repo_owner: GitHub repository owner (optional, uses env var if not provided)
        repo_name: Repository name (optional, uses env var if not provided)
    
    Returns:
        List of opened file paths
    """
    fetcher = GitHubFetcher(repo_owner=repo_owner, repo_name=repo_name)
    return fetcher.list_and_open_documents(doc_types=["requirements", "requirement"])


def open_tech_specs(repo_owner: Optional[str] = None, repo_name: Optional[str] = None) -> List[str]:
    """
    Fetch and open technical specifications from GitHub repository.
    
    Args:
        repo_owner: GitHub repository owner (optional, uses env var if not provided)
        repo_name: Repository name (optional, uses env var if not provided)
    
    Returns:
        List of opened file paths
    """
    fetcher = GitHubFetcher(repo_owner=repo_owner, repo_name=repo_name)
    return fetcher.list_and_open_documents(doc_types=["tech_spec", "technical_spec", "technical_specification"])


def open_all_documents(repo_owner: Optional[str] = None, repo_name: Optional[str] = None) -> List[str]:
    """
    Fetch and open all documents (user stories, requirements, tech specs) from GitHub.
    
    Args:
        repo_owner: GitHub repository owner (optional, uses env var if not provided)
        repo_name: Repository name (optional, uses env var if not provided)
    
    Returns:
        List of opened file paths
    """
    fetcher = GitHubFetcher(repo_owner=repo_owner, repo_name=repo_name)
    return fetcher.list_and_open_documents(
        doc_types=["user_stories", "user_story", "requirements", "requirement", 
                  "tech_spec", "technical_spec", "technical_specification"]
    )


def list_available_documents(repo_owner: Optional[str] = None, repo_name: Optional[str] = None) -> dict:
    """
    List all available documents without opening them.
    
    Args:
        repo_owner: GitHub repository owner (optional, uses env var if not provided)
        repo_name: Repository name (optional, uses env var if not provided)
    
    Returns:
        Dictionary mapping document types to lists of files
    """
    fetcher = GitHubFetcher(repo_owner=repo_owner, repo_name=repo_name)
    return fetcher.find_documents()


if __name__ == "__main__":
    """Quick test/demo of the integration."""
    import sys
    
    if len(sys.argv) > 1:
        command = sys.argv[1].lower()
        
        if command == "user-stories":
            open_user_stories()
        elif command == "requirements":
            open_requirements()
        elif command == "tech-specs":
            open_tech_specs()
        elif command == "all":
            open_all_documents()
        elif command == "list":
            docs = list_available_documents()
            print("\n=== Available Documents ===")
            for doc_type, files in docs.items():
                if files:
                    print(f"\n{doc_type.upper()}:")
                    for file in files:
                        print(f"  - {file['path']}")
        else:
            print(f"Unknown command: {command}")
            print("Usage: python github_integration.py [user-stories|requirements|tech-specs|all|list]")
    else:
        print("Usage: python github_integration.py [user-stories|requirements|tech-specs|all|list]")
