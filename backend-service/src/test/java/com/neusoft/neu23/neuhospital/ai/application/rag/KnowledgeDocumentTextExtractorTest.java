package com.neusoft.neu23.neuhospital.ai.application.rag;

import com.neusoft.neu23.neuhospital.file.entity.FileRecordEntity;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeDocumentTextExtractorTest {

    @Test
    void shouldExtractMarkdownText() throws Exception {
        KnowledgeDocumentTextExtractor extractor = new KnowledgeDocumentTextExtractor();
        FileRecordEntity fileRecord = fileRecord("guide.md", "md", "text/markdown");

        String markdown = """
                # 挂号指南
                初诊患者请先实名建档。
                ## 取号
                请按预约时间提前到院。
                """;

        ExtractedKnowledgeText extracted = extractor.extract(
                fileRecord,
                new ByteArrayInputStream(markdown.getBytes(StandardCharsets.UTF_8))
        );

        assertTrue(extracted.text().contains("挂号指南"));
        assertTrue(extracted.text().contains("初诊患者请先实名建档"));
        assertTrue(extracted.text().contains("请按预约时间提前到院"));
    }

    @Test
    void shouldExtractDocxText() throws Exception {
        KnowledgeDocumentTextExtractor extractor = new KnowledgeDocumentTextExtractor();
        FileRecordEntity fileRecord = fileRecord("guide.docx", "docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph title = document.createParagraph();
            title.createRun().setText("就诊须知");
            XWPFParagraph body = document.createParagraph();
            body.createRun().setText("复诊患者请携带既往检查报告。");
            document.write(outputStream);
        }

        ExtractedKnowledgeText extracted = extractor.extract(
                fileRecord,
                new ByteArrayInputStream(outputStream.toByteArray())
        );

        assertTrue(extracted.text().contains("就诊须知"));
        assertTrue(extracted.text().contains("复诊患者请携带既往检查报告"));
    }

    @Test
    void shouldExtractPdfText() throws Exception {
        KnowledgeDocumentTextExtractor extractor = new KnowledgeDocumentTextExtractor();
        FileRecordEntity fileRecord = fileRecord("notice.pdf", "pdf", "application/pdf");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("Check before fasting");
                contentStream.endText();
            }
            document.save(outputStream);
        }

        ExtractedKnowledgeText extracted = extractor.extract(
                fileRecord,
                new ByteArrayInputStream(outputStream.toByteArray())
        );

        assertTrue(extracted.text().contains("Check before fasting"));
    }

    private FileRecordEntity fileRecord(String name, String fileType, String contentType) {
        FileRecordEntity fileRecord = new FileRecordEntity();
        fileRecord.setOriginalName(name);
        fileRecord.setFileType(fileType);
        fileRecord.setContentType(contentType);
        return fileRecord;
    }
}



