def chunk_text(text: str, max_chars: int = 3000):
    chunks = []
    current = ""

    for line in text.splitlines():
        if len(current) + len(line) > max_chars:
            chunks.append(current)
            current = line
        else:
            current += "\n" + line

    if current:
        chunks.append(current)

    return chunks
