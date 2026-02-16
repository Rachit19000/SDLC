from pathlib import Path
import pdfplumber
from docx import Document


def extract_pdf(path: Path) -> str:
    text = []
    with pdfplumber.open(path) as pdf:
        for page in pdf.pages:
            page_text = page.extract_text()
            if page_text:
                text.append(page_text)
    return "\n".join(text)


def extract_docx(path: Path) -> str:
    doc = Document(path)
    return "\n".join([para.text for para in doc.paragraphs])


def extract_txt(path: Path) -> str:
    return path.read_text(encoding="utf-8")
