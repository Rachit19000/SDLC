from pathlib import Path


def write_text(path: str, content: str):
    Path(path).write_text(content, encoding="utf-8")
