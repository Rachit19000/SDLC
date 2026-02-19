import json
import re
import time
from typing import Tuple, Optional
from prompt_builder import build_prompt
from hf_client import call_inference
from models import UserStoriesArtifact


class FlowController:
    def __init__(self, max_retries: int = 3, model: str = "Qwen/Qwen2.5-7B-Instruct"):
        self.max_retries = max_retries
        self.model = model

    def _strip_markdown_codeblock(self, text: str) -> str:
        """Strip markdown code block markers (```json ... ```) from LLM output."""
        text = text.strip()
        match = re.search(r'```(?:json)?\s*\n?(.*?)```', text, re.DOTALL)
        if match:
            return match.group(1).strip()
        return text

    def _extract_json_object(self, text: str) -> str:
        """Extract JSON object from text that may contain extra content."""
        text = self._strip_markdown_codeblock(text)
        # Find the first { and last }
        start = text.find('{')
        end = text.rfind('}')
        if start != -1 and end != -1 and end > start:
            return text[start:end + 1]
        return text

    def run(self, requirements_json: str) -> Tuple[UserStoriesArtifact, dict]:
        attempts = 0
        start = time.time()
        last_errors: Optional[str] = None

        while attempts < self.max_retries:
            attempts += 1
            prompt = build_prompt(requirements_json, last_validation_errors=last_errors)
            raw = call_inference(prompt, model=self.model)

            # Extract and parse JSON from raw output
            try:
                cleaned = self._extract_json_object(raw)
                parsed = json.loads(cleaned)
            except json.JSONDecodeError:
                last_errors = f"LLM output was not valid JSON. Raw output starts with: {raw[:200]}"
                if attempts >= self.max_retries:
                    raise RuntimeError(f"Failed to parse JSON from LLM after {attempts} attempts: {last_errors}")
                continue

            # Validate with pydantic
            try:
                artifact = UserStoriesArtifact.model_validate(parsed)
                duration = int((time.time() - start) * 1000)
                metadata = {"attempts": attempts, "duration_ms": duration}
                return artifact, metadata

            except Exception as e:
                last_errors = str(e)
                if attempts >= self.max_retries:
                    raise RuntimeError(f"Validation failed after {attempts} attempts: {last_errors}")
