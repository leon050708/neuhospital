package com.neusoft.neu23.neuhospital.ai.application.rag;

import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.file.entity.FileRecordEntity;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class KnowledgeDocumentTextExtractor {

    public ExtractedKnowledgeText extract(FileRecordEntity fileRecord, InputStream inputStream) {
        if (fileRecord == null) {
            throw new BusinessException("知识文件记录不能为空");
        }
        if (inputStream == null) {
            throw new BusinessException("知识文件流不能为空");
        }

        String fileType = resolveFileType(fileRecord);
        String text = switch (fileType) {
            case "md", "markdown", "txt" -> extractMarkdown(inputStream);
            case "docx" -> extractDocx(inputStream);
            case "pdf" -> extractPdf(inputStream);
            default -> throw new BusinessException("暂不支持的知识文档类型: " + fileType);
        };

        return new ExtractedKnowledgeText(fileRecord.getOriginalName(), text.trim());
    }

    private String resolveFileType(FileRecordEntity fileRecord) {
        String fileType = fileRecord.getFileType();
        if (fileType != null && !fileType.isBlank()) {
            return fileType.trim().toLowerCase(Locale.ROOT);
        }
        String originalName = fileRecord.getOriginalName();
        if (originalName == null || !originalName.contains(".")) {
            return "";
        }
        return originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String extractMarkdown(InputStream inputStream) {
        try {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("\r\n", "\n")
                    .replace('\r', '\n')
                    .replaceAll("(?m)^#{1,6}\\s*", "");
        } catch (IOException e) {
            throw new BusinessException("解析 Markdown 失败: " + e.getMessage());
        }
    }

    private String extractDocx(InputStream inputStream) {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        } catch (IOException e) {
            throw new BusinessException("解析 Word 文档失败: " + e.getMessage());
        }
    }

    private String extractPdf(InputStream inputStream) {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new BusinessException("解析 PDF 文档失败: " + e.getMessage());
        }
    }
}
