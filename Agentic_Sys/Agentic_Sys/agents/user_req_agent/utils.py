import json
import os
from pathlib import Path
from typing import Optional, List


def mkdir_p(path: str) -> None:
    Path(path).mkdir(parents=True, exist_ok=True)


def safe_write_json(path: str, obj) -> None:
    mkdir_p(os.path.dirname(path))
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)


def open_github_docs(doc_type: str = "all") -> Optional[List[str]]:
    """
    Quick helper to open GitHub documents in Cursor.
    
    Args:
        doc_type: Type of document to open ('user-stories', 'requirements', 
                 'tech-specs', or 'all')
    
    Returns:
        List of opened file paths, or None if error
    
    Example:
        from utils import open_github_docs
        files = open_github_docs('user-stories')
    """
    try:
        from github_integration import (
            open_user_stories,
            open_requirements,
            open_tech_specs,
            open_all_documents
        )
        
        doc_type_lower = doc_type.lower()
        if doc_type_lower == "user-stories" or doc_type_lower == "user_stories":
            return open_user_stories()
        elif doc_type_lower == "requirements":
            return open_requirements()
        elif doc_type_lower == "tech-specs" or doc_type_lower == "tech_specs":
            return open_tech_specs()
        elif doc_type_lower == "all":
            return open_all_documents()
        else:
            print(f"Unknown document type: {doc_type}")
            print("Use: 'user-stories', 'requirements', 'tech-specs', or 'all'")
            return None
    except ImportError:
        print("GitHub integration not available. Install required packages:")
        print("  pip install requests")
        return None
    except Exception as e:
        print(f"Error opening GitHub documents: {e}")
        return None
