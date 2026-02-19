"""
HuggingFace LLM client for user_req_agent.
Uses chat_completion API with HF_TOKEN and multiple model fallbacks.
"""

import os
import sys
import time
import logging
from typing import Optional
from dotenv import load_dotenv, find_dotenv
from huggingface_hub import InferenceClient

LOGGER = logging.getLogger(__name__)

# Load .env – handle BOM-corrupted files gracefully
load_dotenv(find_dotenv(usecwd=True))

HF_TOKEN = os.getenv("HF_TOKEN")

# If BOM-corrupted key exists, try to recover
if HF_TOKEN is None:
    for key, val in os.environ.items():
        if key.endswith("HF_TOKEN") and key != "HF_TOKEN":
            HF_TOKEN = val
            print("[user_req_agent] Recovered HF_TOKEN from BOM-corrupted env var", file=sys.stderr)
            break

# Also try HUGGINGFACE_TOKEN as alias
if not HF_TOKEN:
    HF_TOKEN = os.getenv("HUGGINGFACE_TOKEN")

if not HF_TOKEN:
    print("[user_req_agent] WARNING: HF_TOKEN not found in .env", file=sys.stderr)

# Models ordered by preference
MODELS = [
    "Qwen/Qwen2.5-7B-Instruct",
    "mistralai/Mistral-7B-Instruct-v0.3",
    "google/gemma-2-9b-it",
    "microsoft/Phi-3-mini-4k-instruct",
    "meta-llama/Meta-Llama-3-8B-Instruct",
]

_clients = {}
_model_unavailable = set()
_permission_denied = False


def call_inference(prompt: str, model: str = None, token: Optional[str] = None,
                   temperature: float = 0.2, max_tokens: int = 4096) -> str:
    """Call HuggingFace InferenceClient with automatic model fallback."""
    global _permission_denied

    token = token or HF_TOKEN
    if not token:
        raise RuntimeError(
            "HF_TOKEN not found in environment. "
            "Create a .env file with HF_TOKEN=hf_... in the agent directory."
        )

    # Build ordered list of models to try
    models_to_try = []
    if model and model not in _model_unavailable:
        models_to_try.append(model)
    for m in MODELS:
        if m not in models_to_try and m not in _model_unavailable:
            models_to_try.append(m)

    last_error = None

    for m in models_to_try:
        if _permission_denied:
            break

        print(f"[user_req_agent] Trying model: {m}...", file=sys.stderr)

        for attempt in range(2):
            try:
                if m not in _clients:
                    _clients[m] = InferenceClient(model=m, token=token)

                response = _clients[m].chat_completion(
                    messages=[{"role": "user", "content": prompt}],
                    max_tokens=max_tokens,
                    temperature=temperature,
                )
                text = response.choices[0].message.content
                if text and text.strip():
                    print(f"[user_req_agent] Success with model: {m}", file=sys.stderr)
                    return text.strip()

                print(f"[user_req_agent] {m}: empty response", file=sys.stderr)
                break

            except Exception as e:
                err = str(e)
                last_error = err

                if "sufficient permissions" in err or "Inference Providers" in err:
                    _permission_denied = True
                    raise RuntimeError(
                        "HF token lacks 'Inference Providers' permission. "
                        "Go to https://huggingface.co/settings/tokens → edit → "
                        "enable 'Make calls to Inference Providers' → Save"
                    )
                elif "403" in err or "gated" in err.lower():
                    _model_unavailable.add(m)
                    print(f"[user_req_agent] {m}: access denied – skipping", file=sys.stderr)
                    break
                elif "404" in err or "not found" in err.lower():
                    _model_unavailable.add(m)
                    print(f"[user_req_agent] {m}: not found – skipping", file=sys.stderr)
                    break
                elif "429" in err or "rate" in err.lower() or "quota" in err.lower():
                    print(f"[user_req_agent] {m}: rate limited", file=sys.stderr)
                    if attempt == 0:
                        time.sleep(5)
                elif "503" in err or "loading" in err.lower():
                    print(f"[user_req_agent] {m}: model loading", file=sys.stderr)
                    if attempt == 0:
                        time.sleep(10)
                else:
                    print(f"[user_req_agent] {m} error: {err[:300]}", file=sys.stderr)
                    if attempt == 0:
                        time.sleep(3)

    raise RuntimeError(f"All HuggingFace models failed. Last error: {last_error}")
