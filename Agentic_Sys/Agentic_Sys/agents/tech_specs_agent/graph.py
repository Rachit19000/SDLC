import re
from langgraph.graph import StateGraph, END
from typing import TypedDict, Optional
from pydantic import ValidationError
from schemas.tech_spec import TechSpecArtifact
from llm import get_llm


MAX_ATTEMPTS = 3
llm = get_llm()


class GraphState(TypedDict):
    requirement_text: str
    user_stories: str
    draft: Optional[str]
    validated: Optional[TechSpecArtifact]
    error: Optional[str]
    attempts: int


def generate(state: GraphState):
    error_context = ""
    if state.get("error"):
        error_context = f"""

IMPORTANT: Your previous attempt failed validation with this error:
{state["error"]}

Please fix the issues and try again. Make sure your output is STRICT JSON matching the schema exactly.
"""

    prompt = f"""You are a senior software architect.

From the software requirements text and user stories below, generate a detailed Technical Specification document.

The technical specification must include:

1. **System Overview**: Project name, description, architecture pattern (e.g., Microservices, Monolith, Layered), and key design decisions.

2. **Data Model**: Define entities with their fields (name, type, description) and relationships between entities.

3. **API Design**: Define REST API endpoints with HTTP method (GET/POST/PUT/PATCH/DELETE), path, description, request body description, and response body description.

4. **Components**: List system components/modules with their name, responsibility, and dependencies on other components.

5. **Tech Stack**: Recommended frontend, backend, database, and infrastructure technologies.

6. **Risks and Mitigations**: Identify technical risks with their impact level and mitigation strategies.

7. **Open Questions**: List any items that need stakeholder clarification.

Rules:
- Output STRICT JSON only
- No explanations, no markdown, no comments
- Every component MUST have a non-empty responsibility
- At least one entity in the data model
- API endpoints method must be one of: GET, POST, PUT, PATCH, DELETE
- At least one component is required
{error_context}
Schema: {TechSpecArtifact.model_json_schema()}

Requirements Text:
{state["requirement_text"]}

User Stories:
{state["user_stories"]}"""

    response = llm.invoke(prompt)
    return {
        "draft": response.content,
        "attempts": state["attempts"] + 1,
        "error": None,
    }


def strip_markdown_codeblock(text: str) -> str:
    """Strip markdown code block markers (```json ... ```) from LLM output."""
    text = text.strip()
    # Remove ```json ... ``` or ``` ... ```
    match = re.search(r'```(?:json)?\s*\n?(.*?)```', text, re.DOTALL)
    if match:
        return match.group(1).strip()
    return text


def validate(state: GraphState):
    try:
        draft = strip_markdown_codeblock(state["draft"])
        artifact = TechSpecArtifact.model_validate_json(draft)
        return {"validated": artifact}
    except ValidationError as e:
        return {"error": str(e), "validated": None}
    except Exception as e:
        return {"error": f"JSON parse error: {str(e)}", "validated": None}


def decide(state: GraphState):
    if state["validated"]:
        return "success"
    if state["attempts"] >= MAX_ATTEMPTS:
        raise RuntimeError(
            f"Failed to generate valid tech spec after {MAX_ATTEMPTS} attempts:\n"
            f"{state.get('error')}"
        )
    return "retry"


def build_graph():
    graph = StateGraph(GraphState)
    graph.add_node("generate", generate)
    graph.add_node("validate", validate)

    graph.set_entry_point("generate")

    graph.add_edge("generate", "validate")
    graph.add_conditional_edges("validate", decide, {"success": END, "retry": "generate"})

    return graph.compile()
