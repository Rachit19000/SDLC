import json
from typing import Optional
from models import UserStoriesArtifact


SYSTEM_INSTRUCTIONS = (
    "You are a sprint planning assistant.\n"
    "Transform functional requirements into weekly sprint-planned user stories (weeks 1-4).\n"
    "Output STRICT JSON only that exactly matches the provided JSON schema.\n"
    "Do NOT include any markdown, explanation, or extra fields.\n"
    "Do NOT invent stories not implied by the requirements.\n"
    "Do NOT include JSON schema definitions ($schema, definitions, properties) in your output.\n"
    "Output only the data object.\n"
    "Distribute stories across 4 sprints logically (complex items later, foundational first).\n"
    "Each user story must link to one or more FR ids from the input requirements.\n"
)


def build_prompt(requirements_json: str, last_validation_errors: Optional[str] = None) -> str:
    schema = UserStoriesArtifact.model_json_schema()

    prompt = [
        "SYSTEM:\n",
        SYSTEM_INSTRUCTIONS,
        "\nUSER:\n",
        "Here are the functional requirements to convert into sprint-planned user stories:\n\n",
        requirements_json,
        "\n\n",
        "The JSON schema for the required output (UserStoriesArtifact) is:\n",
        json.dumps(schema, indent=2),
        "\n\n",
        "Produce a single JSON object that validates against the schema.\n",
        "Return only JSON, no markdown code blocks. Ensure each user story is assigned to a sprint (week 1-4).\n",
    ]

    if last_validation_errors:
        prompt.extend([
            "\n\nLast validation errors (revise to fix):\n",
            last_validation_errors,
            "\n\nPlease produce corrected JSON that addresses the errors above.\n",
        ])

    return "".join(prompt)
