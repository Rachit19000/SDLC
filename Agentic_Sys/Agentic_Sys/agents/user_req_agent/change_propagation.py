"""
Change Propagation Agent - Bidirectional sync between requirements, user stories,
tech specs, and code.

When ANY document in the chain changes, this agent propagates the change to all
related documents:

    Requirements <-> User Stories <-> Tech Specs

Change propagation rules:
  1. Requirements changed   -> Re-generate user stories -> Re-generate tech specs
  2. User Stories changed    -> Re-generate tech specs, update requirements
  3. Tech Specs changed      -> Update user stories, update requirements
  4. Code changed            -> Analyze impact, update requirements/stories/specs

All changes are saved locally and pushed to GitHub.
"""

import os
import sys
import json
import re
import time
import subprocess
import hashlib
from pathlib import Path
from typing import Dict, List, Optional, Tuple
from datetime import datetime
from dotenv import load_dotenv

# Add parent paths for agent imports
AGENTS_DIR = Path(__file__).parent.parent
sys.path.insert(0, str(AGENTS_DIR / "user_req_agent"))

from github_fetcher import GitHubFetcher

# Load environment
load_dotenv(AGENTS_DIR / ".env")
load_dotenv(Path(__file__).parent / ".env")


# ─────────────────────────────────────────────
# Document Type Detection
# ─────────────────────────────────────────────

DOC_TYPE_PATTERNS = {
    "requirements": ["_requirements.md", "_requirements.txt", "requirements.md"],
    "user_stories": ["_user_stories.md", "_user_stories.txt", "user_stories.md"],
    "tech_spec": ["_tech_spec.md", "_tech_specs.md", "tech_spec.md", "tech_specs.md"],
    "parsed": ["_parsed.md"],
}


def detect_document_type(file_path: str) -> Optional[str]:
    """Detect the type of document from its filename."""
    name = Path(file_path).name.lower()
    for doc_type, patterns in DOC_TYPE_PATTERNS.items():
        for pattern in patterns:
            if name.endswith(pattern):
                return doc_type
    return None


def get_base_name(file_path: str) -> str:
    """
    Extract the base name from a document filename by removing the doc type suffix.
    E.g., 'Avni_SmartGovProj_Req_1_parsed.md' -> 'Avni_SmartGovProj_Req_1'
    """
    name = Path(file_path).stem  # remove .md
    for doc_type, patterns in DOC_TYPE_PATTERNS.items():
        for pattern in patterns:
            suffix = pattern.replace(".md", "").replace(".txt", "")
            if name.endswith(suffix):
                return name[: -len(suffix)].rstrip("_")
    return name


def find_original_document(
    base_name: str, target_doc_type: str, github_docs_root: Path
) -> Optional[Path]:
    """
    Search across ALL folders in github_docs to find the original file
    matching the base_name and target document type.
    E.g., find 'Avni_SmartGovProj_Req_1_user_stories.md' in any subfolder.
    """
    patterns = DOC_TYPE_PATTERNS.get(target_doc_type, [])
    for f in github_docs_root.rglob("*"):
        if f.is_file():
            for pattern in patterns:
                if f.name.lower().endswith(pattern):
                    # Check if the base name matches
                    f_base = get_base_name(str(f))
                    if f_base.lower() == base_name.lower():
                        return f
    return None


def find_related_documents(
    changed_file: str, github_docs_root: Path
) -> Dict[str, Optional[Path]]:
    """
    Find related documents across ALL folders in github_docs (not just same folder).
    Matches documents by their base name (e.g., 'Avni_SmartGovProj_Req_1').
    """
    base_name = get_base_name(changed_file)
    related: Dict[str, Optional[Path]] = {}

    for doc_type in DOC_TYPE_PATTERNS:
        related[doc_type] = find_original_document(
            base_name, doc_type, github_docs_root
        )

    return related


# ─────────────────────────────────────────────
# LLM Calling (via MCP Host or direct subprocess)
# ─────────────────────────────────────────────


def call_agent_via_subprocess(
    agent_name: str, agent_dir: Path, entry_file: str, arguments: dict
) -> dict:
    """
    Call an agent via subprocess (same mechanism as MCP host).
    """
    venv_python = agent_dir / "venv" / "Scripts" / "python.exe"

    if not venv_python.exists():
        # Fallback: try system python
        venv_python = "python"

    command = [str(venv_python), entry_file]

    print(f"  >> Calling {agent_name}...", flush=True)
    start_time = time.time()

    result = subprocess.run(
        command,
        cwd=str(agent_dir),
        capture_output=True,
        text=True,
        input=json.dumps(arguments),
        timeout=120,  # 2 minute timeout
    )

    duration = time.time() - start_time
    print(f"  >> {agent_name} completed in {duration:.1f}s", flush=True)

    if result.returncode != 0:
        # Try to get error from stdout
        try:
            output = json.loads(result.stdout)
            if "error" in output:
                raise RuntimeError(f"{agent_name} error: {output['error']}")
        except (json.JSONDecodeError, KeyError):
            pass
        raise RuntimeError(f"{agent_name} failed: {result.stderr[:500]}")

    try:
        output = json.loads(result.stdout)
        if isinstance(output, dict) and output.get("status") == "error":
            raise RuntimeError(f"{agent_name} error: {output.get('error', 'Unknown')}")
        return output
    except json.JSONDecodeError:
        return {"raw_output": result.stdout}


def call_requirements_agent(requirement_text: str) -> dict:
    """Generate structured requirements from text."""
    agent_dir = AGENTS_DIR / "user_req_agent"
    return call_agent_via_subprocess(
        "user-req-agent",
        agent_dir,
        "server.py",
        {"requirement_text": requirement_text},
    )


def call_user_stories_agent(requirements_json: str) -> dict:
    """Generate user stories from requirements."""
    agent_dir = AGENTS_DIR / "user_stories_agent"
    return call_agent_via_subprocess(
        "user-stories-agent",
        agent_dir,
        "server.py",
        {"requirements_json": requirements_json},
    )


def call_tech_specs_agent(requirement_text: str, user_stories: str) -> dict:
    """Generate tech specs from requirements + user stories."""
    agent_dir = AGENTS_DIR / "tech_specs_agent"
    return call_agent_via_subprocess(
        "tech-specs-agent",
        agent_dir,
        "server.py",
        {"requirement_text": requirement_text, "user_stories": user_stories},
    )


# ─────────────────────────────────────────────
# Change Propagation Engine
# ─────────────────────────────────────────────


class ChangePropagationEngine:
    """
    Orchestrates bidirectional change propagation between
    requirements, user stories, and tech specs.
    """

    def __init__(self, fetcher: Optional[GitHubFetcher] = None):
        self.fetcher = fetcher or GitHubFetcher()
        self.github_docs_root = self.fetcher.github_docs_root
        self._file_hashes: Dict[str, str] = {}

    def _get_file_hash(self, file_path: Path) -> str:
        """Get MD5 hash of file content."""
        with open(file_path, "rb") as f:
            return hashlib.md5(f.read()).hexdigest()

    def _read_file(self, file_path: Path) -> str:
        """Read file content."""
        with open(file_path, "r", encoding="utf-8") as f:
            return f.read()

    def _save_file(self, file_path: Path, content: str):
        """Save content to file."""
        file_path.parent.mkdir(parents=True, exist_ok=True)
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"  >> Saved: {file_path.name}", flush=True)

    def _get_base_name(self, file_path: Path) -> str:
        """Extract base name from a document file."""
        return get_base_name(str(file_path))

    def _find_original(self, base_name: str, doc_type: str) -> Optional[Path]:
        """Find the original document file across all folders."""
        return find_original_document(base_name, doc_type, self.github_docs_root)

    def _save_to_original(
        self, base_name: str, doc_type: str, content: str, fallback_folder: Path
    ) -> Optional[Path]:
        """
        Save content to the ORIGINAL file if it exists, otherwise create in fallback folder.
        Returns the path where the file was saved.
        """
        original = self._find_original(base_name, doc_type)
        if original:
            # Overwrite the original file in its existing location
            self._save_file(original, content)
            print(f"     >> Updated original: {original.relative_to(self.github_docs_root)}", flush=True)
            return original
        else:
            # No original found — create in the same folder as the changed file
            suffix_map = {
                "requirements": "_requirements.md",
                "user_stories": "_user_stories.md",
                "tech_spec": "_tech_spec.md",
                "parsed": "_parsed.md",
            }
            new_file = fallback_folder / f"{base_name}{suffix_map.get(doc_type, '.md')}"
            self._save_file(new_file, content)
            print(f"     >> Created new: {new_file.name} (no original found)", flush=True)
            return new_file

    def _push_files_to_github(self, file_paths: List[Path]):
        """Push specific files to GitHub (not an entire folder)."""
        print("\n  >> Pushing changes to GitHub...")
        for f in file_paths:
            if f.exists() and f.is_file():
                try:
                    repo_path = str(
                        f.relative_to(self.github_docs_root)
                    ).replace("\\", "/")
                    self.fetcher.push_file(str(f), repo_path)
                    print(f"  [OK] Pushed: {repo_path}", flush=True)
                except Exception as e:
                    print(f"  [FAIL] Push failed for {f.name}: {e}", flush=True)

    def propagate_requirements_change(
        self, requirements_file: Path, push_to_github: bool = True
    ) -> Dict[str, str]:
        """
        When requirements change:
        1. Re-generate user stories → overwrite ORIGINAL user_stories file
        2. Re-generate tech specs → overwrite ORIGINAL tech_spec file
        3. Push updated originals to GitHub
        """
        print(f"\n{'='*60}")
        print(f"  PROPAGATING: Requirements Change")
        print(f"  File: {requirements_file.name}")
        print(f"{'='*60}")

        base_name = self._get_base_name(requirements_file)
        requirement_text = self._read_file(requirements_file)
        results = {"requirements": str(requirements_file)}
        changed_files = [requirements_file]

        # Step 1: Generate user stories → overwrite original
        print("\n  [Step 1/2] Generating user stories from updated requirements...")
        try:
            stories_result = call_user_stories_agent(requirement_text)
            stories_markdown = stories_result.get("user_stories", "")

            if stories_markdown:
                saved_path = self._save_to_original(
                    base_name, "user_stories", stories_markdown, requirements_file.parent
                )
                if saved_path:
                    results["user_stories"] = str(saved_path)
                    changed_files.append(saved_path)
            else:
                print("  [WARN] User stories generation returned empty result")
        except Exception as e:
            print(f"  [FAIL] User stories generation failed: {e}")
            stories_markdown = ""

        # Step 2: Generate tech specs → overwrite original
        print("\n  [Step 2/2] Generating tech specs from updated requirements + stories...")
        try:
            user_stories_text = stories_markdown or ""
            specs_result = call_tech_specs_agent(requirement_text, user_stories_text)
            specs_markdown = specs_result.get("tech_spec", "")

            if specs_markdown:
                saved_path = self._save_to_original(
                    base_name, "tech_spec", specs_markdown, requirements_file.parent
                )
                if saved_path:
                    results["tech_spec"] = str(saved_path)
                    changed_files.append(saved_path)
            else:
                print("  [WARN] Tech spec generation returned empty result")
        except Exception as e:
            print(f"  [FAIL] Tech spec generation failed: {e}")

        # Step 3: Push only the changed files to GitHub
        if push_to_github:
            self._push_files_to_github(changed_files)

        return results

    def propagate_user_stories_change(
        self, stories_file: Path, push_to_github: bool = True
    ) -> Dict[str, str]:
        """
        When user stories change:
        1. Re-generate tech specs → overwrite ORIGINAL tech_spec file
        2. Push updated originals to GitHub
        """
        print(f"\n{'='*60}")
        print(f"  PROPAGATING: User Stories Change")
        print(f"  File: {stories_file.name}")
        print(f"{'='*60}")

        base_name = self._get_base_name(stories_file)
        stories_text = self._read_file(stories_file)
        results = {"user_stories": str(stories_file)}
        changed_files = [stories_file]

        # Find existing requirements from ANY folder (by base name)
        requirement_text = self._find_and_read_original(base_name, "requirements")
        if not requirement_text:
            requirement_text = self._find_and_read_original(base_name, "parsed")

        if not requirement_text:
            print("  [WARN] No requirements found. Using stories as context.")
            requirement_text = stories_text

        # Step 1: Generate tech specs → overwrite original
        print("\n  [Step 1/1] Generating tech specs from requirements + updated stories...")
        try:
            specs_result = call_tech_specs_agent(requirement_text, stories_text)
            specs_markdown = specs_result.get("tech_spec", "")

            if specs_markdown:
                saved_path = self._save_to_original(
                    base_name, "tech_spec", specs_markdown, stories_file.parent
                )
                if saved_path:
                    results["tech_spec"] = str(saved_path)
                    changed_files.append(saved_path)
            else:
                print("  [WARN] Tech spec generation returned empty result")
        except Exception as e:
            print(f"  [FAIL] Tech spec generation failed: {e}")

        # Push only the changed files to GitHub
        if push_to_github:
            self._push_files_to_github(changed_files)

        return results

    def propagate_tech_spec_change(
        self, spec_file: Path, push_to_github: bool = True
    ) -> Dict[str, str]:
        """
        When tech specs change:
        1. Re-generate user stories → overwrite ORIGINAL user_stories file
        2. Push updated originals to GitHub
        """
        print(f"\n{'='*60}")
        print(f"  PROPAGATING: Tech Spec Change")
        print(f"  File: {spec_file.name}")
        print(f"{'='*60}")

        base_name = self._get_base_name(spec_file)
        spec_text = self._read_file(spec_file)
        results = {"tech_spec": str(spec_file)}
        changed_files = [spec_file]

        # Find existing requirements from ANY folder
        requirement_text = self._find_and_read_original(base_name, "requirements")
        if not requirement_text:
            requirement_text = self._find_and_read_original(base_name, "parsed")

        if not requirement_text:
            print("  [WARN] No requirements found. Cannot back-propagate from tech spec alone.")
            return results

        # Step 1: Re-generate user stories → overwrite original
        print("\n  [Step 1/1] Re-generating user stories with tech spec context...")

        enriched_requirements = (
            f"{requirement_text}\n\n"
            f"--- Technical Specification Context (updated) ---\n\n"
            f"{spec_text}"
        )

        try:
            stories_result = call_user_stories_agent(enriched_requirements)
            stories_markdown = stories_result.get("user_stories", "")

            if stories_markdown:
                saved_path = self._save_to_original(
                    base_name, "user_stories", stories_markdown, spec_file.parent
                )
                if saved_path:
                    results["user_stories"] = str(saved_path)
                    changed_files.append(saved_path)
            else:
                print("  [WARN] User stories generation returned empty result")
        except Exception as e:
            print(f"  [FAIL] User stories generation failed: {e}")

        # Push only the changed files to GitHub
        if push_to_github:
            self._push_files_to_github(changed_files)

        return results

    def propagate_parsed_change(
        self, parsed_file: Path, push_to_github: bool = True
    ) -> Dict[str, str]:
        """
        When a parsed/raw requirement document changes:
        1. Re-generate structured requirements → overwrite ORIGINAL
        2. Re-generate user stories → overwrite ORIGINAL
        3. Re-generate tech specs → overwrite ORIGINAL
        """
        print(f"\n{'='*60}")
        print(f"  PROPAGATING: Raw Document Change (Full Pipeline)")
        print(f"  File: {parsed_file.name}")
        print(f"{'='*60}")

        base_name = self._get_base_name(parsed_file)
        raw_text = self._read_file(parsed_file)
        results = {"parsed": str(parsed_file)}
        changed_files = [parsed_file]

        # Show which originals will be updated
        print(f"\n  Base name: {base_name}")
        for dt in ["requirements", "user_stories", "tech_spec"]:
            orig = self._find_original(base_name, dt)
            if orig:
                print(f"  >> Will update: {orig.relative_to(self.github_docs_root)}")
            else:
                print(f"  >> Will create new: {dt} (no original found)")

        # Step 1: Generate structured requirements → overwrite original
        print("\n  [Step 1/3] Generating structured requirements...")
        try:
            req_result = call_requirements_agent(raw_text)
            req_markdown = req_result.get("requirements_text", "")
            req_json = req_result.get("requirements", {})

            if req_markdown:
                saved_path = self._save_to_original(
                    base_name, "requirements", req_markdown, parsed_file.parent
                )
                if saved_path:
                    results["requirements"] = str(saved_path)
                    changed_files.append(saved_path)
            else:
                print("  [WARN] Requirements generation returned empty result")
                return results
        except Exception as e:
            print(f"  [FAIL] Requirements generation failed: {e}")
            return results

        # Step 2: Generate user stories → overwrite original
        print("\n  [Step 2/3] Generating user stories...")
        stories_markdown = ""
        try:
            req_input = json.dumps(req_json) if req_json else req_markdown
            stories_result = call_user_stories_agent(req_input)
            stories_markdown = stories_result.get("user_stories", "")

            if stories_markdown:
                saved_path = self._save_to_original(
                    base_name, "user_stories", stories_markdown, parsed_file.parent
                )
                if saved_path:
                    results["user_stories"] = str(saved_path)
                    changed_files.append(saved_path)
            else:
                print("  [WARN] User stories generation returned empty result")
        except Exception as e:
            print(f"  [FAIL] User stories generation failed: {e}")

        # Step 3: Generate tech specs → overwrite original
        print("\n  [Step 3/3] Generating tech specs...")
        try:
            specs_result = call_tech_specs_agent(
                req_markdown or raw_text, stories_markdown
            )
            specs_markdown = specs_result.get("tech_spec", "")

            if specs_markdown:
                saved_path = self._save_to_original(
                    base_name, "tech_spec", specs_markdown, parsed_file.parent
                )
                if saved_path:
                    results["tech_spec"] = str(saved_path)
                    changed_files.append(saved_path)
            else:
                print("  [WARN] Tech spec generation returned empty result")
        except Exception as e:
            print(f"  [FAIL] Tech spec generation failed: {e}")

        # Push only the changed files to GitHub
        if push_to_github:
            self._push_files_to_github(changed_files)

        return results

    def _find_and_read_original(
        self, base_name: str, doc_type: str
    ) -> Optional[str]:
        """Find and read the original document of a given type across all folders."""
        original = self._find_original(base_name, doc_type)
        if original and original.exists():
            return self._read_file(original)
        return None

    def propagate_change(
        self, changed_file: str, push_to_github: bool = True
    ) -> Dict[str, str]:
        """
        Main entry point: detect document type and propagate changes accordingly.
        """
        changed_path = Path(changed_file)
        doc_type = detect_document_type(str(changed_path))

        if doc_type is None:
            print(f"  [WARN] Cannot determine document type for: {changed_path.name}")
            print(f"  Supported patterns: _requirements.md, _user_stories.md, _tech_spec.md, _parsed.md")
            return {}

        print(f"\n  Detected document type: {doc_type.upper()}")

        if doc_type == "requirements":
            return self.propagate_requirements_change(changed_path, push_to_github)
        elif doc_type == "user_stories":
            return self.propagate_user_stories_change(changed_path, push_to_github)
        elif doc_type == "tech_spec":
            return self.propagate_tech_spec_change(changed_path, push_to_github)
        elif doc_type == "parsed":
            return self.propagate_parsed_change(changed_path, push_to_github)
        else:
            print(f"  [WARN] Unknown document type: {doc_type}")
            return {}

    def scan_and_build_hashes(self) -> Dict[str, str]:
        """Scan all document files and build hash map."""
        hashes = {}
        if self.github_docs_root.exists():
            for f in self.github_docs_root.rglob("*"):
                if f.is_file() and f.suffix in (".md", ".txt"):
                    hashes[str(f)] = self._get_file_hash(f)
        return hashes

    def detect_changes(self) -> List[str]:
        """Detect files that have changed since last scan."""
        current_hashes = self.scan_and_build_hashes()
        changed = []

        for file_path, file_hash in current_hashes.items():
            old_hash = self._file_hashes.get(file_path)
            if old_hash is not None and old_hash != file_hash:
                changed.append(file_path)

        self._file_hashes = current_hashes
        return changed

    def watch_and_propagate(self, debounce_seconds: float = 5.0):
        """
        Watch for document changes and automatically propagate.
        Runs indefinitely until Ctrl+C.
        """
        print("=" * 60)
        print("  Change Propagation Watcher")
        print("=" * 60)
        print(f"\n  Watching:  {self.github_docs_root}")
        print(f"  Repo:      {self.fetcher.repo_owner}/{self.fetcher.repo_name}")
        print(f"  Branch:    {self.fetcher.branch}")
        print(f"  Debounce:  {debounce_seconds}s")
        print(f"\n  How it works:")
        print(f"  - Edit a requirements doc -> User stories + tech specs auto-update")
        print(f"  - Edit user stories       -> Tech specs auto-update")
        print(f"  - Edit tech specs          -> User stories auto-update")
        print(f"  - All changes pushed to GitHub automatically")
        print(f"\n  Press Ctrl+C to stop")
        print("-" * 60)

        # Initial scan
        self._file_hashes = self.scan_and_build_hashes()
        doc_count = len(self._file_hashes)
        print(f"  >> Tracking {doc_count} document(s)")
        print("-" * 60)

        pending_changes: Dict[str, float] = {}

        try:
            while True:
                changed_files = self.detect_changes()

                if changed_files:
                    now = time.time()
                    for f in changed_files:
                        # Only track documents that we can propagate
                        if detect_document_type(f) is not None:
                            pending_changes[f] = now

                # Process changes after debounce
                now = time.time()
                ready = [
                    f
                    for f, t in pending_changes.items()
                    if now - t >= debounce_seconds
                ]

                if ready:
                    timestamp = time.strftime("%H:%M:%S")
                    print(
                        f"\n[{timestamp}] Detected {len(ready)} document change(s):"
                    )

                    for file_path in ready:
                        doc_type = detect_document_type(file_path)
                        print(f"  - {Path(file_path).name} ({doc_type})")

                        try:
                            self.propagate_change(file_path, push_to_github=True)
                        except Exception as e:
                            print(f"  [FAIL] Propagation error: {e}")

                        del pending_changes[file_path]

                    # Re-scan hashes after propagation (new files may have been created)
                    self._file_hashes = self.scan_and_build_hashes()

                    print(f"\n[{timestamp}] Propagation complete [OK]")

                time.sleep(1)

        except KeyboardInterrupt:
            print("\n\n>> Watcher stopped.")


# ─────────────────────────────────────────────
# CLI
# ─────────────────────────────────────────────


def main():
    import argparse

    parser = argparse.ArgumentParser(
        description="Change Propagation Agent - bidirectional document sync"
    )
    parser.add_argument(
        "command",
        choices=["propagate", "watch", "detect"],
        help="Command to run",
    )
    parser.add_argument(
        "--file",
        help="File to propagate changes for (required for 'propagate')",
    )
    parser.add_argument(
        "--no-push",
        action="store_true",
        help="Don't push changes to GitHub",
    )
    parser.add_argument(
        "--debounce",
        type=float,
        default=5.0,
        help="Debounce seconds for watch mode (default: 5)",
    )

    args = parser.parse_args()

    try:
        engine = ChangePropagationEngine()

        if args.command == "propagate":
            if not args.file:
                print("Error: --file is required for 'propagate' command")
                return 1

            results = engine.propagate_change(
                args.file, push_to_github=not args.no_push
            )

            print(f"\n{'='*60}")
            print(f"  Propagation Results:")
            for doc_type, path in results.items():
                print(f"  [OK] {doc_type}: {Path(path).name}")
            print(f"{'='*60}")

        elif args.command == "watch":
            engine.watch_and_propagate(debounce_seconds=args.debounce)

        elif args.command == "detect":
            engine._file_hashes = engine.scan_and_build_hashes()
            print(f"Scanned {len(engine._file_hashes)} documents")
            for f in sorted(engine._file_hashes.keys()):
                doc_type = detect_document_type(f) or "unknown"
                print(f"  [{doc_type:15}] {Path(f).name}")

        return 0

    except ValueError as e:
        print(f"Configuration Error: {e}")
        return 1
    except Exception as e:
        print(f"Error: {e}")
        return 1


if __name__ == "__main__":
    exit(main())
