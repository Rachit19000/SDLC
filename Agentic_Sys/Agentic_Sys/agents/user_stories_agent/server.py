"""
user_stories_agent server – reads JSON from stdin, generates sprint-planned user stories,
outputs JSON to stdout (for MCP host consumption).

Input (via stdin JSON):
  { "requirements_json": "...", "job_id": "..." }

Output (via stdout JSON):
  {
    "user_stories": "formatted markdown",
    "user_stories_json": { ... UserStoriesArtifact as dict ... },
    "status": "success"
  }
"""

import sys
import json
import time
from dotenv import load_dotenv

load_dotenv()

# Capture the real stdout before anything else can pollute it
_real_stdout = sys.stdout
# Redirect stdout to stderr so library logs don't corrupt our JSON output
sys.stdout = sys.stderr

from langgraph_flow import FlowController
from models import UserStoriesArtifact


def format_user_stories_markdown(artifact: UserStoriesArtifact) -> str:
    """Format the user stories artifact as readable markdown."""
    lines = [f"# User Stories: {artifact.system_name}\n"]
    lines.append(f"**Total User Stories:** {artifact.total_user_stories}")
    lines.append(f"**Estimated Effort:** {artifact.estimated_effort_days} days\n")

    for sprint in artifact.sprints:
        lines.append(f"## Sprint Week {sprint.week}: {sprint.theme}\n")
        for story in sprint.user_stories:
            lines.append(f"### {story.id}: {story.title}")
            lines.append(f"{story.description}\n")
            lines.append("**Acceptance Criteria:**")
            for ac in story.acceptance_criteria:
                lines.append(f"- {ac}")
            linked = ", ".join(story.linked_fr_ids)
            lines.append(f"\n**Linked FRs:** {linked}\n")

    return "\n".join(lines)


def main():
    try:
        # Read arguments from stdin (sent by MCP host)
        raw_input = sys.stdin.read()
        args = json.loads(raw_input)
    except json.JSONDecodeError as e:
        sys.stdout = _real_stdout
        print(json.dumps({"status": "error", "error": f"Invalid JSON arguments: {str(e)}"}))
        return

    requirements_json = args.get("requirements_json", "")

    if not requirements_json:
        sys.stdout = _real_stdout
        print(json.dumps({"status": "error", "error": "No requirements_json provided"}))
        return

    # If requirements_json is a dict (already parsed), convert to string
    if isinstance(requirements_json, dict):
        requirements_json = json.dumps(requirements_json)

    try:
        flow = FlowController()
        start = time.time()
        artifact, metadata = flow.run(requirements_json)
        duration_ms = int((time.time() - start) * 1000)

        # Format as markdown
        markdown = format_user_stories_markdown(artifact)

        # Build output
        output = {
            "status": "success",
            "user_stories": markdown,
            "user_stories_json": json.loads(artifact.model_dump_json()),
            "total_user_stories": artifact.total_user_stories,
            "sprints": len(artifact.sprints),
            "estimated_effort_days": artifact.estimated_effort_days,
            "attempts": metadata.get("attempts"),
            "duration_ms": duration_ms,
        }

        # Restore real stdout for the final JSON output only
        sys.stdout = _real_stdout
        print(json.dumps(output))

    except Exception as e:
        sys.stdout = _real_stdout
        print(json.dumps({"status": "error", "error": str(e)}))


if __name__ == "__main__":
    main()
