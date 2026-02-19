import json
from typing import Optional
from models import RequirementsArtifact


SYSTEM_INSTRUCTIONS = (
    "You are a requirements extraction assistant.\n"
    "Extract Functional Requirements (FR), Non-Functional Requirements (NFR), and Acceptance Criteria (AC) from the provided text.\n"
    "Output STRICT JSON only that exactly matches the provided JSON schema.\n"
    "Do NOT include any markdown, explanation, or extra fields.\n"
    "Do NOT invent features not implied by the text.\n"
    "Do NOT include JSON schema definitions ($schema, definitions, properties) in your output.\n"
    "Output only the data object.\n"
)


def build_prompt(extracted_text: str, last_validation_errors: Optional[str] = None) -> str:
    schema = RequirementsArtifact.model_json_schema()

    prompt = [
        "SYSTEM:\n",
        SYSTEM_INSTRUCTIONS,
        "\nUSER:\n",
        "Here is the extracted text to analyze:\n\n",
        extracted_text,
        "\n\n",
        "The JSON schema for the required output (RequirementsArtifact) is:\n",
        json.dumps(schema, indent=2),
        "\n\n",
        "Produce a single JSON object that validates against the schema.\n",
        "Return only JSON, no markdown code blocks.\n",
    ]

    if last_validation_errors:
        prompt.extend([
            "\n\nLast validation errors (revise to fix):\n",
            last_validation_errors,
            "\n\nPlease produce corrected JSON that addresses the errors above.\n",
        ])

    return "".join(prompt)
