from pathlib import Path
import json


def read_text(path: str) -> str:
    return Path(path).read_text(encoding="utf-8")


def write_json(path: str, data: dict):
    Path(path).write_text(
        json.dumps(data, indent=2),
        encoding="utf-8"
    )
