import sys
import json
import io
from dotenv import load_dotenv

load_dotenv()

# Capture the real stdout before anything else can pollute it
_real_stdout = sys.stdout
# Redirect stdout to stderr during module loading so that
# gradio_client's "Loaded as API: ..." messages don't corrupt our JSON output
sys.stdout = sys.stderr

from graph import build_graph
from schemas.requirements import RequirementsArtifact


def format_user_stories_markdown(artifact: RequirementsArtifact) -> str:
    """Format the requirements artifact as readable markdown."""
    lines = ["# Generated User Stories\n"]

    # User Stories
    lines.append("## User Stories\n")
    for story in artifact.user_stories:
        lines.append(f"### {story.id}")
        lines.append(f"**As a** {story.role}, **I want** {story.feature}, **so that** {story.benefit}\n")

    # Acceptance Criteria
    lines.append("## Acceptance Criteria\n")
    for ac in artifact.acceptance_criteria:
        lines.append(f"### {ac.story_id}")
        for criterion in ac.criteria:
            lines.append(f"- {criterion}")
        lines.append("")

    # Assumptions
    lines.append("## Assumptions\n")
    for assumption in artifact.assumptions:
        lines.append(f"- {assumption}")

    return "\n".join(lines)


def main():
    try:
        # Read arguments from stdin (sent by MCP host to avoid Windows CLI length limit)
        raw_input = sys.stdin.read()
        args = json.loads(raw_input)
    except json.JSONDecodeError as e:
        # Restore stdout for final JSON output only
        sys.stdout = _real_stdout
        print(json.dumps({"error": f"Invalid JSON arguments: {str(e)}"}))
        sys.exit(1)

    requirement_text = args.get("requirement_text", "")

    if not requirement_text:
        sys.stdout = _real_stdout
        print(json.dumps({"error": "No requirement_text provided"}))
        sys.exit(1)

    graph = build_graph()

    state = {
        "extracted_text": requirement_text,
        "draft": None,
        "validated": None,
        "error": None,
        "attempts": 0,
    }

    result = graph.invoke(state)
    artifact: RequirementsArtifact = result["validated"]

    # Format as markdown for GitHub upload
    markdown = format_user_stories_markdown(artifact)

    output = {
        "status": "success",
        "user_stories": markdown,
        "user_stories_count": len(artifact.user_stories),
        "result": artifact.model_dump(),
    }

    # Restore real stdout for the final JSON output only
    sys.stdout = _real_stdout
    print(json.dumps(output))


if __name__ == "__main__":
    main()
