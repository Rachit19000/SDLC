"""
user_req_agent server – reads JSON from stdin, generates structured requirements,
outputs JSON to stdout (for MCP host consumption).

Input (via stdin JSON):
  { "requirement_text": "...", "job_id": "..." }

Output (via stdout JSON):
  {
    "requirements": { ... RequirementsArtifact as dict ... },
    "requirements_text": "formatted markdown",
    "status": "success"
  }
"""

import sys
import json
import io
import time
from dotenv import load_dotenv

load_dotenv()

# Capture the real stdout before anything else can pollute it
_real_stdout = sys.stdout
# Redirect stdout to stderr so library logs don't corrupt our JSON output
sys.stdout = sys.stderr

from langgraph_flow import FlowController
from models import RequirementsArtifact


def format_requirements_markdown(artifact: RequirementsArtifact) -> str:
    """Format the requirements artifact as readable markdown."""
    lines = [f"# Requirements: {artifact.system_name}\n"]

    # Functional Requirements
    lines.append("## Functional Requirements\n")
    for fr in artifact.functional_requirements:
        lines.append(f"### {fr.id}: {fr.title}")
        lines.append(f"{fr.description}\n")

    # Non-Functional Requirements
    lines.append("## Non-Functional Requirements\n")
    if artifact.nonfunctional_requirements:
        for nfr in artifact.nonfunctional_requirements:
            lines.append(f"- **{nfr.id}**: {nfr.description}")
    else:
        lines.append("_No non-functional requirements identified._")
    lines.append("")

    # Acceptance Criteria
    lines.append("## Acceptance Criteria\n")
    for ac in artifact.acceptance_criteria:
        refs = ", ".join(ac.references)
        lines.append(f"### {ac.id} (References: {refs})")
        lines.append(f"- {ac.text}\n")

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

    requirement_text = args.get("requirement_text", "")

    if not requirement_text:
        sys.stdout = _real_stdout
        print(json.dumps({"status": "error", "error": "No requirement_text provided"}))
        return

    try:
        flow = FlowController()
        start = time.time()
        artifact, metadata = flow.run(requirement_text)
        duration_ms = int((time.time() - start) * 1000)

        # Format as markdown
        markdown = format_requirements_markdown(artifact)

        # Build output
        output = {
            "status": "success",
            "requirements": json.loads(artifact.model_dump_json()),
            "requirements_text": markdown,
            "functional_requirements_count": len(artifact.functional_requirements),
            "nonfunctional_requirements_count": len(artifact.nonfunctional_requirements),
            "acceptance_criteria_count": len(artifact.acceptance_criteria),
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
