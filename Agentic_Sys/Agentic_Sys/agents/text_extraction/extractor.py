from pathlib import Path
from parsers import extract_pdf, extract_docx, extract_txt
from cleaners import clean_text_llm
from chunking import chunk_text


def extract_text(file_path: str, use_llm_cleanup: bool = False) -> str:
    path = Path(file_path)
    suffix = path.suffix.lower()

    if suffix == ".pdf":
        raw_text = extract_pdf(path)
    elif suffix == ".docx":
        raw_text = extract_docx(path)
    elif suffix == ".txt":
        raw_text = extract_txt(path)
    else:
        raise ValueError(f"Unsupported file type: {suffix}")

    if use_llm_cleanup:
        chunks = chunk_text(raw_text)
        cleaned_chunks = [
            clean_text_llm(chunk) for chunk in chunks
        ]
        return "\n".join(cleaned_chunks)

    return raw_text
