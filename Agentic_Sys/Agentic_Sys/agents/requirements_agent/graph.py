import re
from langgraph.graph import StateGraph, END
from typing import TypedDict, Optional
from pydantic import ValidationError
from schemas.requirements import RequirementsArtifact
from llm import get_llm


MAX_ATTEMPTS = 3
llm = get_llm()

class GraphState(TypedDict):
    extracted_text: str
    draft: Optional[str]
    validated: Optional[RequirementsArtifact]
    error: Optional[str]
    attempts: int

def generate(state: GraphState):
    prompt = f"""
You are a senior product manager.

From the software requirements text below:

1. Generate user stories in the format:
   As a <role>, I want <feature>, so that <benefit>

2. For each user story, generate acceptance criteria that are:
   - testable
   - unambiguous
   - complete

3. Explicitly list assumptions you made.

Rules:
- Every user story MUST have acceptance criteria
- Acceptance criteria MUST reference the user story ID
- Output STRICT JSON only
- No explanations, no markdown

Schema: {RequirementsArtifact.model_json_schema()}
Requirements_Text: {
    state["extracted_text"]   
}"""
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
        artifact = RequirementsArtifact.model_validate_json(draft)
        return {"validated": artifact}
    except ValidationError as e:
        return {"error": str(e), "validated": None}
def decide(state : GraphState):
    if state["validated"]:
       return "success"
    if state["attempts"]>= MAX_ATTEMPTS:
        raise RuntimeError(
            f"Failed to generate valid requirements after {MAX_ATTEMPTS} attempts:\n"
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