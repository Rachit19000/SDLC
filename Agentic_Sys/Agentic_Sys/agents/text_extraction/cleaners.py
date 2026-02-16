import os
import time
from dotenv import load_dotenv
from huggingface_hub import InferenceClient

load_dotenv()

HF_TOKEN = os.getenv("HF_TOKEN")
MODEL = "Qwen/Qwen2.5-7B-Instruct"

client = InferenceClient(model=MODEL, token=HF_TOKEN)


def clean_text_llm(text: str) -> str:
    prompt = f"""
Clean the following extracted document text.

Rules:
- Fix broken line breaks
- Remove headers and footers
- Preserve meaning exactly
- Do NOT summarize
- Do NOT rewrite
- Do NOT change wording
- Return ONLY cleaned text
- No explanations

Text:
{text}
"""

    max_retries = 3
    for attempt in range(max_retries):
        try:
            # Use chat_completion for conversational models
            response = client.chat_completion(
                messages=[{"role": "user", "content": prompt}],
                max_tokens=2048,
                temperature=0.0,
            )
            return response.choices[0].message.content.strip()
        except Exception as e:
            if "503" in str(e) or "loading" in str(e).lower():
                wait_time = 10 * (attempt + 1)
                time.sleep(wait_time)
                continue
            elif attempt == max_retries - 1:
                raise RuntimeError(f"Failed to clean text: {e}")
            else:
                continue
    
    raise RuntimeError(f"Failed to get response after {max_retries} attempts")
