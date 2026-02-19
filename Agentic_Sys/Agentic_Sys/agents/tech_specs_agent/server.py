import sys
import json
from dotenv import load_dotenv

load_dotenv()

from graph import build_graph
from schemas.tech_spec import TechSpecArtifact


def format_tech_spec_markdown(artifact: TechSpecArtifact) -> str:
    """Format the tech spec artifact as readable markdown."""
    lines = ["# Technical Specification\n"]

    # System Overview
    overview = artifact.system_overview
    lines.append("## 1. System Overview\n")
    lines.append(f"**Project Name:** {overview.name}\n")
    lines.append(f"**Description:** {overview.description}\n")
    lines.append(f"**Architecture Pattern:** {overview.architecture_pattern}\n")
    if overview.key_decisions:
        lines.append("### Key Design Decisions\n")
        for decision in overview.key_decisions:
            lines.append(f"- {decision}")
        lines.append("")

    # Data Model
    lines.append("## 2. Data Model\n")
    for entity in artifact.data_model.entities:
        lines.append(f"### Entity: {entity.name}\n")
        lines.append("| Field | Type | Description |")
        lines.append("|-------|------|-------------|")
        for field in entity.fields:
            lines.append(f"| {field.name} | {field.type} | {field.description} |")
        if entity.relationships:
            lines.append(f"\n**Relationships:** {', '.join(entity.relationships)}")
        lines.append("")

    # API Design
    lines.append("## 3. API Design\n")
    lines.append("| Method | Path | Description |")
    lines.append("|--------|------|-------------|")
    for endpoint in artifact.api_design.endpoints:
        lines.append(f"| {endpoint.method} | `{endpoint.path}` | {endpoint.description} |")
    lines.append("")

    for endpoint in artifact.api_design.endpoints:
        lines.append(f"### {endpoint.method} `{endpoint.path}`\n")
        lines.append(f"{endpoint.description}\n")
        if endpoint.request_body:
            lines.append(f"**Request Body:** {endpoint.request_body}\n")
        if endpoint.response_body:
            lines.append(f"**Response Body:** {endpoint.response_body}\n")

    # Components
    lines.append("## 4. Component Breakdown\n")
    for component in artifact.components:
        lines.append(f"### {component.name}\n")
        lines.append(f"**Responsibility:** {component.responsibility}\n")
        if component.depends_on:
            lines.append(f"**Depends on:** {', '.join(component.depends_on)}\n")

    # Tech Stack
    ts = artifact.tech_stack
    lines.append("## 5. Tech Stack\n")
    lines.append("| Layer | Technology |")
    lines.append("|-------|-----------|")
    if ts.frontend:
        lines.append(f"| Frontend | {ts.frontend} |")
    if ts.backend:
        lines.append(f"| Backend | {ts.backend} |")
    if ts.database:
        lines.append(f"| Database | {ts.database} |")
    if ts.infrastructure:
        lines.append(f"| Infrastructure | {ts.infrastructure} |")
    lines.append("")

    # Risks and Mitigations
    if artifact.risks_and_mitigations:
        lines.append("## 6. Risks & Mitigations\n")
        lines.append("| Risk | Impact | Mitigation |")
        lines.append("|------|--------|------------|")
        for risk in artifact.risks_and_mitigations:
            lines.append(f"| {risk.risk} | {risk.impact} | {risk.mitigation} |")
        lines.append("")

    # Open Questions
    if artifact.open_questions:
        lines.append("## 7. Open Questions\n")
        for q in artifact.open_questions:
            lines.append(f"- {q}")
        lines.append("")

    return "\n".join(lines)


def main():
    try:
        # Read arguments from stdin (sent by MCP host)
        raw_input = sys.stdin.read()
        args = json.loads(raw_input)
    except json.JSONDecodeError as e:
        print(json.dumps({"error": f"Invalid JSON arguments: {str(e)}"}))
        sys.exit(1)

    requirement_text = args.get("requirement_text", "")
    user_stories = args.get("user_stories", "")

    if not requirement_text:
        print(json.dumps({"error": "No requirement_text provided"}))
        sys.exit(1)

    graph = build_graph()

    state = {
        "requirement_text": requirement_text,
        "user_stories": user_stories,
        "draft": None,
        "validated": None,
        "error": None,
        "attempts": 0,
    }

    result = graph.invoke(state)
    artifact: TechSpecArtifact = result["validated"]

    # Format as markdown for GitHub upload
    markdown = format_tech_spec_markdown(artifact)

    output = {
        "status": "success",
        "tech_spec": markdown,
        "result": artifact.model_dump(),
    }

    print(json.dumps(output))


if __name__ == "__main__":
    main()
