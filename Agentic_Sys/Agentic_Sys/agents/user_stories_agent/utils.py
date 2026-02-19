import json
import os
from pathlib import Path


def mkdir_p(path: str) -> None:
    Path(path).mkdir(parents=True, exist_ok=True)


def safe_write_json(path: str, obj) -> None:
    mkdir_p(os.path.dirname(path))
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
