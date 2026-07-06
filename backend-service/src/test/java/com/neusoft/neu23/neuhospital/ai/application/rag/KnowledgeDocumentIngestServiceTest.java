package com.neusoft.neu23.neuhospital.ai.application.rag;

import com.neusoft.neu23.neuhospital.ai.domain.entity.KnowledgeChunkEntity;
import com.neusoft.neu23.neuhospital.ai.domain.entity.KnowledgeDocumentEntity;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.KnowledgeChunkService;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.KnowledgeDocumentService;
import com.neusoft.neu23.neuhospital.file.entity.FileRecordEntity;
import com.neusoft.neu23.neuhospital.file.service.FileService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.embedding.EmbeddingModel;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeDocumentIngestServiceTest {

    @Test
    void shouldParseCleanChunkEmbedAndPersistDocument() {
        KnowledgeDocumentService documentService = mock(KnowledgeDocumentService.class);
        KnowledgeChunkService chunkService = mock(KnowledgeChunkService.class);
        FileService fileService = mock(FileService.class);
        KnowledgeDocumentTextExtractor extractor = mock(KnowledgeDocumentTextExtractor.class);
        KnowledgeTextCleaner cleaner = mock(KnowledgeTextCleaner.class);
        KnowledgeChunker chunker = mock(KnowledgeChunker.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);

        KnowledgeDocumentIngestService ingestService = new KnowledgeDocumentIngestService(
                documentService,
                chunkService,
                fileService,
                extractor,
                cleaner,
                chunker,
                embeddingModel,
                "text-embedding-v3"
        );

        KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
        document.setId(11L);
        document.setTitle("挂号流程");
        document.setFileRecordId(200L);
        document.setKnowledgeType("REGISTRATION_PROCESS");
        document.setDepartmentId(12L);
        document.setTags("{挂号,初诊}");
        document.setStatus("DRAFT");
        document.setParserStatus("PENDING");
        when(documentService.getById(11L)).thenReturn(document);

        FileRecordEntity fileRecord = new FileRecordEntity();
        fileRecord.setOriginalName("guide.md");
        fileRecord.setFileType("md");
        fileRecord.setContentType("text/markdown");
        when(fileService.downloadFile(200L)).thenReturn(new FileService.DownloadFile(
                fileRecord,
                new ByteArrayInputStream("raw".getBytes(StandardCharsets.UTF_8))
        ));

        when(extractor.extract(eq(fileRecord), any())).thenReturn(new ExtractedKnowledgeText("guide.md", "raw"));
        when(cleaner.clean("raw")).thenReturn("clean text");
        when(chunker.chunk("clean text")).thenReturn(List.of("chunk one", "chunk two"));
        when(embeddingModel.embed("chunk one")).thenReturn(new float[]{1.0f, 2.0f});
        when(embeddingModel.embed("chunk two")).thenReturn(new float[]{3.0f, 4.0f});
        when(chunkService.saveBatch(any())).thenReturn(true);

        int chunkCount = ingestService.ingestDocument(11L);

        assertEquals(2, chunkCount);
        verify(chunkService).remove(any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<KnowledgeChunkEntity>> chunkCaptor = ArgumentCaptor.forClass(List.class);
        verify(chunkService).saveBatch(chunkCaptor.capture());
        assertEquals(2, chunkCaptor.getValue().size());
        assertEquals("chunk one", chunkCaptor.getValue().get(0).getContentText());
        assertEquals("chunk one", chunkCaptor.getValue().get(0).getLegacyChunkText());
        assertEquals("text-embedding-v3", chunkCaptor.getValue().get(0).getEmbeddingModel());
        assertTrue(chunkCaptor.getValue().get(0).getEmbeddingText().contains("1.0"));

        ArgumentCaptor<KnowledgeDocumentEntity> documentCaptor = ArgumentCaptor.forClass(KnowledgeDocumentEntity.class);
        verify(documentService, atLeastOnce()).updateById(documentCaptor.capture());
        KnowledgeDocumentEntity finalState = documentCaptor.getValue();
        assertEquals("EMBEDDED", finalState.getParserStatus());
        assertEquals(2, finalState.getChunkCount());
    }
}
