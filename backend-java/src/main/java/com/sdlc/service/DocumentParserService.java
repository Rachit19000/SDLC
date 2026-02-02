package com.sdlc.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class DocumentParserService {
    
    private static final Logger log = LoggerFactory.getLogger(DocumentParserService.class);
    
    public ParseResult parseDocument(MultipartFile file) throws IOException {
        log.info("📄 Parsing document: {} ({})", file.getOriginalFilename(), file.getContentType());
        
        String mimeType = file.getContentType();
        String fileName = file.getOriginalFilename();
        
        if (mimeType == null) {
            throw new IllegalArgumentException("File type cannot be determined");
        }
        
        String extractedText;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileName", fileName);
        metadata.put("mimeType", mimeType);
        metadata.put("fileSize", file.getSize());
        metadata.put("parsedAt", Instant.now().toString());
        
        // Parse based on file type
        if (mimeType.equals("application/pdf")) {
            extractedText = parsePdf(file.getInputStream(), metadata);
        } else if (mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") 
                || fileName.endsWith(".docx")) {
            extractedText = parseDocx(file.getInputStream(), metadata);
        } else if (mimeType.equals("text/plain") || fileName.endsWith(".txt")) {
            extractedText = parseTxt(file.getInputStream(), metadata);
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + mimeType + ". Supported: PDF, DOCX, TXT");
        }
        
        // Validate and clean text
        if (extractedText == null || extractedText.trim().isEmpty()) {
            throw new RuntimeException("No text content could be extracted from the document");
        }
        
        extractedText = cleanText(extractedText);
        
        log.info("✅ Document parsed: {} characters extracted", extractedText.length());
        
        return new ParseResult(extractedText, metadata);
    }
    
    private String parsePdf(InputStream inputStream, Map<String, Object> metadata) throws IOException {
        log.info("📄 Parsing PDF file...");
        
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            
            metadata.put("pages", document.getNumberOfPages());
            
            log.info("✅ PDF parsed: {} pages, {} characters", 
                    document.getNumberOfPages(), text.length());
            
            return text;
        }
    }
    
    private String parseDocx(InputStream inputStream, Map<String, Object> metadata) throws IOException {
        log.info("📝 Parsing Word document...");
        
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            
            String text = extractor.getText();
            
            log.info("✅ Word document parsed: {} characters", text.length());
            
            return text;
        }
    }
    
    private String parseTxt(InputStream inputStream, Map<String, Object> metadata) throws IOException {
        log.info("📄 Parsing text file...");
        
        String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        
        log.info("✅ Text file parsed: {} characters", text.length());
        
        return text;
    }
    
    private String cleanText(String text) {
        return text
                .replaceAll("\r\n", "\n")          // Normalize line breaks
                .replaceAll("\r", "\n")            // Handle old Mac line breaks
                .replaceAll("\n{3,}", "\n\n")      // Remove excessive blank lines
                .trim();
    }
    
    public boolean isSupportedFileType(String mimeType, String fileName) {
        if (fileName == null) {
            return false;
        }
        
        return mimeType != null && (
                mimeType.equals("application/pdf") ||
                mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                mimeType.equals("text/plain")
        ) || fileName.endsWith(".pdf") || fileName.endsWith(".docx") || fileName.endsWith(".txt");
    }
    
    public static class ParseResult {
        private final String text;
        private final Map<String, Object> metadata;
        
        public ParseResult(String text, Map<String, Object> metadata) {
            this.text = text;
            this.metadata = metadata;
        }
        
        public String getText() {
            return text;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }
}
