package com.neusoft.neu23.neuhospital.ai.application.rag;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.neu23.neuhospital.ai.domain.entity.KnowledgeChunkEntity;
import com.neusoft.neu23.neuhospital.ai.domain.entity.KnowledgeDocumentEntity;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.KnowledgeChunkService;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.KnowledgeDocumentService;
import com.neusoft.neu23.neuhospital.ct.config.MinioProperties;
import com.neusoft.neu23.neuhospital.file.service.FileService;
import com.neusoft.neu23.neuhospital.file.vo.FileRecordVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeAdminServiceTest {

    @Test
    void shouldUploadCreateDocumentAndTriggerIngest() {
        FileService fileService = mock(FileService.class);
        KnowledgeDocumentService documentService = mock(KnowledgeDocumentService.class);
        KnowledgeChunkService chunkService = mock(KnowledgeChunkService.class);
        KnowledgeDocumentIngestService ingestService = mock(KnowledgeDocumentIngestService.class);
        MinioProperties minioProperties = new MinioProperties();
        MinioProperties.Bucket bucket = new MinioProperties.Bucket();
        bucket.setKnowledgeDocs("knowledge-docs");
        minioProperties.setBucket(bucket);

        KnowledgeAdminService adminService = new KnowledgeAdminService(
                fileService,
                documentService,
                chunkService,
                ingestService,
                minioProperties
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "guide.md",
                "text/markdown",
                "# 挂号指南".getBytes()
        );

        FileRecordVO fileRecordVO = new FileRecordVO();
        fileRecordVO.setId(88L);
        when(fileService.uploadFile(eq(file), eq("KNOWLEDGE_DOCUMENT"), eq(null),
                eq("knowledge-docs"), eq("knowledge/"), eq(301L))).thenReturn(fileRecordVO);
        doAnswer(invocation -> {
            KnowledgeDocumentEntity entity = invocation.getArgument(0);
            entity.setId(101L);
            return true;
        }).when(documentService).save(any(KnowledgeDocumentEntity.class));
        when(ingestService.ingestDocument(101L)).thenReturn(1);

        KnowledgeDocumentEntity created = adminService.createDocumentFromUpload(
                file,
                new KnowledgeDocumentUploadCommand("挂号流程", "REGISTRATION_PROCESS", 12L, "挂号,初诊", true),
                301L
        );

        assertNotNull(created);
        assertEquals(101L, created.getId());

        ArgumentCaptor<KnowledgeDocumentEntity> captor = ArgumentCaptor.forClass(KnowledgeDocumentEntity.class);
        verify(documentService).save(captor.capture());
        assertEquals("挂号流程", captor.getValue().getTitle());
        assertEquals("PUBLISHED", captor.getValue().getStatus());
        assertEquals(88L, captor.getValue().getFileRecordId());

        verify(ingestService).ingestDocument(101L);
    }

    @Test
    void shouldSyncChunkStatusWhenPublishingDraftDocument() {
        FileService fileService = mock(FileService.class);
        KnowledgeDocumentService documentService = mock(KnowledgeDocumentService.class);
        KnowledgeChunkService chunkService = mock(KnowledgeChunkService.class);
        KnowledgeDocumentIngestService ingestService = mock(KnowledgeDocumentIngestService.class);
        MinioProperties minioProperties = new MinioProperties();

        KnowledgeAdminService adminService = new KnowledgeAdminService(
                fileService,
                documentService,
                chunkService,
                ingestService,
                minioProperties
        );

        KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
        document.setId(101L);
        document.setStatus("DRAFT");
        document.setParserStatus("EMBEDDED");
        document.setChunkCount(3);
        when(documentService.getById(101L)).thenReturn(document);

        adminService.publishDocument(101L, 9001L);

        verify(chunkService).update(
                argThat(update -> "PUBLISHED".equals(update.getDocumentStatus())),
                argThat((QueryWrapper<KnowledgeChunkEntity> wrapper) ->
                        wrapper.getSqlSegment().contains("document_id") && wrapper.getSqlSegment().contains("deleted"))
        );
    }

    @Test
    void shouldSyncChunkStatusWhenOffliningPublishedDocument() {
        FileService fileService = mock(FileService.class);
        KnowledgeDocumentService documentService = mock(KnowledgeDocumentService.class);
        KnowledgeChunkService chunkService = mock(KnowledgeChunkService.class);
        KnowledgeDocumentIngestService ingestService = mock(KnowledgeDocumentIngestService.class);
        MinioProperties minioProperties = new MinioProperties();

        KnowledgeAdminService adminService = new KnowledgeAdminService(
                fileService,
                documentService,
                chunkService,
                ingestService,
                minioProperties
        );

        KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
        document.setId(101L);
        document.setStatus("PUBLISHED");
        when(documentService.getById(101L)).thenReturn(document);

        adminService.offlineDocument(101L, 9001L);

        verify(chunkService).update(
                argThat(update -> "OFFLINE".equals(update.getDocumentStatus())),
                argThat((QueryWrapper<KnowledgeChunkEntity> wrapper) ->
                        wrapper.getSqlSegment().contains("document_id") && wrapper.getSqlSegment().contains("deleted"))
        );
    }

    @Test
    void shouldReindexDocumentThroughIngestService() {
        FileService fileService = mock(FileService.class);
        KnowledgeDocumentService documentService = mock(KnowledgeDocumentService.class);
        KnowledgeChunkService chunkService = mock(KnowledgeChunkService.class);
        KnowledgeDocumentIngestService ingestService = mock(KnowledgeDocumentIngestService.class);
        MinioProperties minioProperties = new MinioProperties();

        KnowledgeAdminService adminService = new KnowledgeAdminService(
                fileService,
                documentService,
                chunkService,
                ingestService,
                minioProperties
        );

        KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
        document.setId(101L);
        when(documentService.getById(101L)).thenReturn(document);
        when(ingestService.ingestDocument(101L)).thenReturn(4);

        int chunkCount = adminService.reindexDocument(101L, 9001L);

        assertEquals(4, chunkCount);
        verify(documentService).updateById(argThat(update -> update.getId().equals(101L) && update.getUpdatedBy().equals(9001L)));
        verify(ingestService).ingestDocument(101L);
    }
}
