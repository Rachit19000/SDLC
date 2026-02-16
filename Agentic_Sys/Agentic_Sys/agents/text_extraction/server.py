from mcp.server.fastmcp import FastMCP
from extractor import extract_text
from utils import write_text
from dotenv import load_dotenv

load_dotenv()

mcp = FastMCP("text-extraction-agent")

@mcp.tool()
def extract_document(
    input_file_path: str,
    output_text_path: str,
    use_llm_cleanup: bool = False,
):
    """
    Extracts text from document and writes to output path.
    Stateless. Deterministic.
    """

    extracted_text = extract_text(
        input_file_path,
        use_llm_cleanup=use_llm_cleanup
    )

    write_text(output_text_path, extracted_text)

    return {
        "status": "SUCCESS",
        "characters_extracted": len(extracted_text)
    }


if __name__ == "__main__":
    mcp.run()
