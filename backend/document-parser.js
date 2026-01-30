// Document Parser Agent
// Extracts text from PDF, DOCX, and other document formats

const pdfParse = require('pdf-parse');
const mammoth = require('mammoth');

/**
 * Parse document and extract text content
 * @param {Buffer} fileBuffer - File buffer
 * @param {string} mimeType - File MIME type
 * @param {string} fileName - Original file name
 * @returns {Promise<{text: string, metadata: object}>}
 */
async function parseDocument(fileBuffer, mimeType, fileName) {
  try {
    let extractedText = '';
    let metadata = {
      fileName: fileName,
      mimeType: mimeType,
      fileSize: fileBuffer.length,
      parsedAt: new Date().toISOString()
    };

    // Parse based on file type
    if (mimeType === 'application/pdf') {
      console.log('📄 Parsing PDF file...');
      const pdfData = await pdfParse(fileBuffer);
      extractedText = pdfData.text;
      metadata = {
        ...metadata,
        pages: pdfData.numpages,
        info: pdfData.info || {},
        metadata: pdfData.metadata || {}
      };
      console.log(`✅ PDF parsed: ${pdfData.numpages} pages, ${extractedText.length} characters`);
      
    } else if (
      mimeType === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' ||
      mimeType === 'application/msword' ||
      fileName.endsWith('.docx') ||
      fileName.endsWith('.doc')
    ) {
      console.log('📝 Parsing Word document...');
      
      if (mimeType === 'application/msword' || fileName.endsWith('.doc')) {
        // Old .doc format - not directly supported by mammoth
        // Return error message
        throw new Error('Old .doc format is not supported. Please convert to .docx or PDF.');
      }
      
      const result = await mammoth.extractRawText({ buffer: fileBuffer });
      extractedText = result.value;
      
      if (result.messages && result.messages.length > 0) {
        console.log('⚠️ Word parsing warnings:', result.messages);
        metadata.warnings = result.messages;
      }
      
      console.log(`✅ Word document parsed: ${extractedText.length} characters`);
      
    } else if (mimeType === 'text/plain' || fileName.endsWith('.txt')) {
      console.log('📄 Parsing text file...');
      extractedText = fileBuffer.toString('utf-8');
      console.log(`✅ Text file parsed: ${extractedText.length} characters`);
      
    } else {
      throw new Error(`Unsupported file type: ${mimeType}. Supported: PDF, DOCX, TXT`);
    }

    // Clean and validate extracted text
    if (!extractedText || extractedText.trim().length === 0) {
      throw new Error('No text content could be extracted from the document. The file might be empty or corrupted.');
    }

    // Clean up text (remove excessive whitespace, normalize line breaks)
    extractedText = extractedText
      .replace(/\r\n/g, '\n')  // Normalize line breaks
      .replace(/\r/g, '\n')    // Handle old Mac line breaks
      .replace(/\n{3,}/g, '\n\n')  // Remove excessive blank lines
      .trim();

    return {
      text: extractedText,
      metadata: metadata
    };

  } catch (error) {
    console.error('❌ Document parsing error:', error.message);
    throw new Error(`Failed to parse document: ${error.message}`);
  }
}

/**
 * Get file extension from filename
 */
function getFileExtension(fileName) {
  return fileName.split('.').pop().toLowerCase();
}

/**
 * Check if file type is supported
 */
function isSupportedFileType(mimeType, fileName) {
  const supportedTypes = [
    'application/pdf',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/msword',
    'text/plain'
  ];
  
  const supportedExtensions = ['.pdf', '.docx', '.doc', '.txt'];
  const extension = '.' + getFileExtension(fileName);
  
  return supportedTypes.includes(mimeType) || supportedExtensions.includes(extension);
}

module.exports = {
  parseDocument,
  getFileExtension,
  isSupportedFileType
};
