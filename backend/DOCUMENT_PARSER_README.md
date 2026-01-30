# Document Parser Agent

## Overview
The Document Parser Agent extracts text content from uploaded documents (PDF, DOCX, TXT) before storing them in GitHub. This ensures that readable text is stored instead of binary files.

---

## How It Works

### Flow:
1. **User uploads file** (PDF/DOCX/TXT) → Backend receives it
2. **Document Parser Agent** extracts text from the file
3. **Extracted text** is stored in GitHub (not the binary file)
4. **User can read** the text directly in GitHub

---

## Supported File Types

| File Type | Extension | Status |
|-----------|-----------|--------|
| PDF | `.pdf` | ✅ Supported |
| Word Document | `.docx` | ✅ Supported |
| Word Document (old) | `.doc` | ❌ Not supported (convert to .docx) |
| Text File | `.txt` | ✅ Supported |

---

## Features

### PDF Parsing
- Extracts text from all pages
- Preserves page structure
- Returns metadata (page count, document info)

### Word Document Parsing
- Extracts text from .docx files
- Preserves formatting structure
- Handles warnings gracefully

### Text File Parsing
- Direct UTF-8 text extraction
- Normalizes line breaks

---

## File Storage Structure

**Before (Binary files):**
```
requirements/user_1/job_123/document.pdf  ❌ (Binary, can't read)
```

**After (Extracted text):**
```
requirements/user_1/job_123/document_extracted.txt  ✅ (Readable text)
```

---

## API Response

When a file is uploaded and parsed:

```json
{
  "jobId": "job_1703123456789",
  "status": "stored",
  "message": "File parsed and text uploaded successfully to GitHub",
  "githubUrl": "https://github.com/.../document_extracted.txt",
  "commitUrl": "https://github.com/.../commit/abc123",
  "filePath": "requirements/user_1/job_123/document_extracted.txt",
  "fileName": "document_extracted.txt",
  "originalFileName": "document.pdf",
  "extractedTextLength": 1234,
  "metadata": {
    "fileName": "document.pdf",
    "mimeType": "application/pdf",
    "pages": 5,
    "parsedAt": "2024-01-15T10:30:00.000Z"
  }
}
```

---

## Error Handling

### Unsupported File Type
```json
{
  "error": {
    "code": "BAD_REQUEST",
    "message": "Unsupported file type: application/msword. Supported: PDF, DOCX, TXT"
  }
}
```

### Parsing Failure
```json
{
  "error": {
    "code": "INTERNAL_ERROR",
    "message": "Failed to parse document: No text content could be extracted"
  }
}
```

### Empty Document
```json
{
  "error": {
    "code": "INTERNAL_ERROR",
    "message": "No text content could be extracted from the document. The file might be empty or corrupted."
  }
}
```

---

## Technical Details

### Libraries Used
- **pdf-parse**: PDF text extraction
- **mammoth**: DOCX text extraction

### Text Cleaning
- Normalizes line breaks (`\r\n` → `\n`)
- Removes excessive blank lines
- Trims whitespace

### File Size Limits
- Maximum file size: 10MB (configured in multer)
- Large PDFs may take longer to parse

---

## Testing

### Test PDF Upload
1. Upload a PDF file
2. Check backend logs for parsing status
3. Verify text is extracted and stored in GitHub

### Test DOCX Upload
1. Upload a .docx file
2. Verify text extraction
3. Check GitHub for readable text file

---

## Future Enhancements

- [ ] Support for old .doc format
- [ ] Image extraction from PDFs
- [ ] Table extraction
- [ ] OCR for scanned PDFs
- [ ] Support for Excel files (.xlsx)
- [ ] Support for PowerPoint files (.pptx)

---

## Troubleshooting

### "No text content could be extracted"
- File might be corrupted
- File might be image-based (scanned PDF)
- File might be password-protected

### "Unsupported file type"
- Convert file to PDF or DOCX
- Use supported formats only

### Slow parsing
- Large files take longer
- PDFs with many pages take more time
- Consider file size limits
