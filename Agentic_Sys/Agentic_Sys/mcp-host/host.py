import subprocess
import yaml
import json
import os
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI()


# -------------------------
# Load Agent Registry
# -------------------------

def load_registry():
    with open("agent_registry.yaml", "r") as f:
        return yaml.safe_load(f)["agents"]


AGENTS = load_registry()


# -------------------------
# Request / Response Models
# -------------------------

class ExecuteRequest(BaseModel):
    agent: str
    arguments: dict


class ExecuteResponse(BaseModel):
    status: str
    metadata: dict | None = None
    error: str | None = None


# -------------------------
# Execution Logic
# -------------------------

def run_agent(agent_name: str, arguments: dict):

    if agent_name not in AGENTS:
        raise ValueError(f"Unknown agent: {agent_name}")

    agent_info = AGENTS[agent_name]

    agent_path = os.path.abspath(agent_info["path"])
    entry_file = agent_info["entry"]

    # Windows-only venv path
    venv_python = os.path.join(agent_path, "venv", "Scripts", "python.exe")

    if not os.path.exists(venv_python):
        raise RuntimeError(
            f"Python executable not found in venv for agent: {agent_name}"
        )

    command = [
        venv_python,
        entry_file
    ]

    # Pass arguments via stdin (not CLI args) to avoid Windows command-line length limit
    result = subprocess.run(
        command,
        cwd=agent_path,
        capture_output=True,
        text=True,
        input=json.dumps(arguments)
    )

    if result.returncode != 0:
        # Try to get structured error from stdout first
        try:
            output = json.loads(result.stdout)
            if "error" in output:
                raise RuntimeError(output["error"])
        except (json.JSONDecodeError, KeyError):
            pass
        raise RuntimeError(result.stderr)

    try:
        output = json.loads(result.stdout)
        # Check if agent returned an error status
        if isinstance(output, dict) and output.get("status") == "error":
            raise RuntimeError(output.get("error", "Agent returned error status"))
        return output
    except json.JSONDecodeError:
        return {"raw_output": result.stdout}


# -------------------------
# API Endpoint
# -------------------------

@app.post("/execute", response_model=ExecuteResponse)
def execute(request: ExecuteRequest):
    try:
        metadata = run_agent(request.agent, request.arguments)

        return ExecuteResponse(
            status="SUCCESS",
            metadata=metadata
        )

    except Exception as e:
        return ExecuteResponse(
            status="FAILED",
            error=str(e)
        )


# -------------------------
# Health Check
# -------------------------

@app.get("/health")
def health():
    return {"status": "MCP Host Running"}
