"""
LLM wrapper using HuggingFace open-source models only:
  1. Primary:   HuggingFace InferenceClient  – uses HF_TOKEN with open-source models
  2. Fallback:  HuggingFace Gradio Spaces    – free open-source model spaces (GPU quota limited)

IMPORTANT: Your HuggingFace token (Settings > Access Tokens) must have
           the "Make calls to Inference Providers" permission enabled.
"""

import os
import sys
import time
from dotenv import load_dotenv, find_dotenv

# Load .env – handle BOM-corrupted files gracefully
load_dotenv(find_dotenv(usecwd=True))

HF_TOKEN = os.getenv("HF_TOKEN")

# If BOM-corrupted key exists, try to recover
if HF_TOKEN is None:
    for key, val in os.environ.items():
        if key.endswith("HF_TOKEN") and key != "HF_TOKEN":
            HF_TOKEN = val
            print("[LLM] Recovered HF_TOKEN from BOM-corrupted env var", file=sys.stderr)
            break

if not HF_TOKEN:
    print("[LLM] WARNING: HF_TOKEN not found in .env – HuggingFace models will not work", file=sys.stderr)

# ── HuggingFace open-source models (ordered by preference) ──
PRIMARY_MODEL = "Qwen/Qwen2.5-7B-Instruct"
FALLBACK_MODELS = [
    "mistralai/Mistral-7B-Instruct-v0.3",
    "google/gemma-2-9b-it",
    "microsoft/Phi-3-mini-4k-instruct",
    "meta-llama/Meta-Llama-3-8B-Instruct",
]

# ── Gradio Spaces (open-source models, free but GPU-quota limited) ──
GRADIO_SPACES = [
    ("hysts/mistral-7b", "/chat", {"message": "message", "max_tokens": "param_2", "temperature": "param_3"}),
    ("huggingface-projects/gemma-2-9b-it", "/chat", {"message": "message", "max_tokens": "max_new_tokens", "temperature": "temperature"}),
]


class _Response:
    """Simple wrapper so callers can do response.content"""
    __slots__ = ("content",)

    def __init__(self, text: str):
        self.content = text


class HuggingFaceLLM:
    """LLM wrapper using only HuggingFace open-source models."""

    def __init__(self, model: str = PRIMARY_MODEL, token: str = None, temperature: float = 0.2):
        self.model = model
        self.token = token
        self.temperature = temperature
        # lazy-init caches
        self._clients = {}           # model -> InferenceClient
        self._model_available = {}   # model -> bool
        self._permission_denied = False  # True if token lacks Inference Providers perm
        self._gradio_clients = {}

    # =====================================================================
    # Strategy 1 – HuggingFace InferenceClient (open-source models)
    # =====================================================================
    def _try_hf_model(self, prompt: str, model: str) -> str | None:
        """Try a specific HuggingFace model via InferenceClient."""
        if self._permission_denied:
            return None
        if self._model_available.get(model) is False:
            return None
        if not self.token:
            print("[LLM] No HF_TOKEN available – cannot use InferenceClient", file=sys.stderr)
            return None

        # Lazy-init client for this model
        if model not in self._clients:
            try:
                from huggingface_hub import InferenceClient
                self._clients[model] = InferenceClient(model=model, token=self.token)
            except Exception as e:
                print(f"[LLM] Failed to init client for {model}: {e}", file=sys.stderr)
                self._model_available[model] = False
                return None

        client = self._clients[model]
        try:
            response = client.chat_completion(
                messages=[{"role": "user", "content": prompt}],
                max_tokens=4096,
                temperature=self.temperature,
            )
            text = response.choices[0].message.content
            if text and text.strip():
                self._model_available[model] = True
                return text.strip()
            print(f"[LLM] {model}: empty response", file=sys.stderr)
            return None
        except Exception as e:
            err = str(e)
            if "sufficient permissions" in err or "Inference Providers" in err:
                self._permission_denied = True
                print(
                    f"[LLM] ERROR: Your HuggingFace token does not have 'Inference Providers' permission.\n"
                    f"[LLM] Go to https://huggingface.co/settings/tokens → edit your token → "
                    f"enable 'Make calls to Inference Providers' → Save",
                    file=sys.stderr,
                )
                return None
            elif "403" in err or "permission" in err.lower() or "gated" in err.lower():
                self._model_available[model] = False
                print(f"[LLM] {model}: access denied (403/gated) – skipping", file=sys.stderr)
            elif "429" in err or "rate" in err.lower() or "quota" in err.lower():
                print(f"[LLM] {model}: rate limited – will retry", file=sys.stderr)
            elif "404" in err or "not found" in err.lower():
                self._model_available[model] = False
                print(f"[LLM] {model}: model not found (404) – skipping", file=sys.stderr)
            elif "503" in err or "loading" in err.lower():
                print(f"[LLM] {model}: model loading (503) – will retry", file=sys.stderr)
            else:
                print(f"[LLM] {model} error: {err[:300]}", file=sys.stderr)
            return None

    # =====================================================================
    # Strategy 2 – HuggingFace Gradio Spaces (open-source, free)
    # =====================================================================
    def _try_gradio_space(self, prompt: str, space_name: str, api_name: str, param_map: dict) -> str | None:
        if space_name not in self._gradio_clients:
            from gradio_client import Client
            old_token = os.environ.pop("HF_TOKEN", None)
            old_stdout = sys.stdout
            sys.stdout = sys.stderr
            try:
                self._gradio_clients[space_name] = Client(space_name)
            finally:
                sys.stdout = old_stdout
                if old_token is not None:
                    os.environ["HF_TOKEN"] = old_token

        client = self._gradio_clients[space_name]
        kwargs = {
            param_map["message"]: prompt,
            param_map["max_tokens"]: 2048,
            param_map["temperature"]: self.temperature,
            "api_name": api_name,
        }
        old_stdout = sys.stdout
        sys.stdout = sys.stderr
        try:
            return client.predict(**kwargs)
        finally:
            sys.stdout = old_stdout

    # =====================================================================
    # Public API
    # =====================================================================
    def invoke(self, prompt: str) -> _Response:
        last_error: str | None = None

        # --- Strategy 1: HuggingFace InferenceClient (primary model) ---
        if self.token and not self._permission_denied:
            print(f"[LLM] Trying HF model: {self.model}...", file=sys.stderr)
            for attempt in range(3):
                text = self._try_hf_model(prompt, self.model)
                if text:
                    return _Response(text)
                if self._model_available.get(self.model) is False:
                    break
                if self._permission_denied:
                    break
                if attempt < 2:
                    wait = 5 * (attempt + 1)
                    print(f"[LLM] Retrying {self.model} in {wait}s (attempt {attempt+2}/3)...", file=sys.stderr)
                    time.sleep(wait)
            last_error = f"{self.model} unavailable"

            # --- Strategy 1b: HuggingFace InferenceClient (fallback models) ---
            if not self._permission_denied:
                for fallback_model in FALLBACK_MODELS:
                    if self._model_available.get(fallback_model) is False:
                        continue
                    print(f"[LLM] Trying fallback HF model: {fallback_model}...", file=sys.stderr)
                    for attempt in range(2):
                        text = self._try_hf_model(prompt, fallback_model)
                        if text:
                            return _Response(text)
                        if self._model_available.get(fallback_model) is False:
                            break
                        if self._permission_denied:
                            break
                        if attempt == 0:
                            time.sleep(5)
                    if self._permission_denied:
                        break
                    last_error = f"{fallback_model} unavailable"

        if self._permission_denied:
            last_error = (
                "HF token lacks 'Inference Providers' permission. "
                "Go to https://huggingface.co/settings/tokens → edit your token → "
                "enable 'Make calls to Inference Providers' → Save"
            )

        # --- Strategy 2: Gradio Spaces (open-source models, free) ---
        for space_name, api_name, param_map in GRADIO_SPACES:
            print(f"[LLM] Trying Gradio Space: {space_name}...", file=sys.stderr)
            for attempt in range(2):
                try:
                    text = self._try_gradio_space(prompt, space_name, api_name, param_map)
                    if text and str(text).strip():
                        return _Response(str(text).strip())
                except Exception as e:
                    last_error = str(e)
                    err = str(e)
                    if "quota" in err.lower() or "exceeded" in err.lower():
                        print(f"[LLM] GPU quota exceeded for {space_name}", file=sys.stderr)
                        self._gradio_clients.pop(space_name, None)
                        break
                    if attempt == 0:
                        time.sleep(5)
                        self._gradio_clients.pop(space_name, None)
                    else:
                        self._gradio_clients.pop(space_name, None)
                        break

        raise RuntimeError(
            f"All HuggingFace LLM strategies failed. Last error: {last_error}"
        )


def get_llm():
    """Create an LLM instance using HuggingFace open-source models."""
    return HuggingFaceLLM(model=PRIMARY_MODEL, token=HF_TOKEN, temperature=0.2)
