import os
import time
from dotenv import load_dotenv
from huggingface_hub import InferenceClient

load_dotenv()

HF_TOKEN = os.getenv("HF_TOKEN")
# Use the same model as text extraction agent
MODEL = "Qwen/Qwen2.5-7B-Instruct"


class HuggingFaceLLM:
    """Wrapper to use InferenceClient for conversational models."""
    def __init__(self, model: str, token: str, temperature: float = 0.2):
        self.client = InferenceClient(model=model, token=token)
        self.temperature = temperature
    
    def invoke(self, prompt: str) -> str:
        max_retries = 3
        for attempt in range(max_retries):
            try:
                # Try chat_completion first (for conversational models)
                response = self.client.chat_completion(
                    messages=[{"role": "user", "content": prompt}],
                    max_tokens=2048,
                    temperature=self.temperature,
                )
                text = response.choices[0].message.content
            except Exception as e:
                if "503" in str(e) or "loading" in str(e).lower():
                    # Model is loading, wait and retry
                    wait_time = 10 * (attempt + 1)
                    print(f"Model loading, waiting {wait_time}s...")
                    time.sleep(wait_time)
                    continue
                elif attempt == max_retries - 1:
                    # Last attempt failed
                    raise RuntimeError(f"Failed to get response: {e}")
                else:
                    # Try again
                    continue
            
            # Return object with .content attribute for LangChain compatibility
            class Response:
                def __init__(self, text):
                    self.content = text.strip()
            return Response(text)
        
        raise RuntimeError(f"Failed to get response after {max_retries} attempts")


def get_llm():
    return HuggingFaceLLM(
        model=MODEL,
        token=HF_TOKEN,
        temperature=0.2,
    )
