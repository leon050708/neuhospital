package com.neusoft.neu23.neuhospital.ai.application.rag;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.neu23.neuhospital.ai.domain.entity.KnowledgeChunkEntity;
import com.neusoft.neu23.neuhospital.ai.domain.entity.KnowledgeDocumentEntity;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.KnowledgeChunkService;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.KnowledgeDocumentService;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.ct.config.MinioProperties;
import com.neusoft.neu23.neuhospital.file.service.FileService;
import com.neusoft.neu23.neuhospital.file.vo.FileRecordVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class KnowledgeAdminService {

    private final FileService fileService;
    private final KnowledgeDocumentService documentService;
    private final KnowledgeChunkService chunkService;
    private final KnowledgeDocumentIngestService ingestService;
    private final MinioProperties minioProperties;

    public KnowledgeAdminService(FileService fileService,
                                 KnowledgeDocumentService documentService,
                                 KnowledgeChunkService chunkService,
                                 KnowledgeDocumentIngestService ingestService,
                                 MinioProperties minioProperties) {
        this.fileService = fileService;
        this.documentService = documentService;
        this.chunkService = chunkService;
        this.ingestService = ingestService;
        this.minioProperties = minioProperties;
    }

    public KnowledgeDocumentEntity createDocumentFromUpload(MultipartFile file,
                                                            KnowledgeDocumentUploadCommand command,
                                                            Long operatorId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("知识文档文件不能为空");
        }
        if (command == null || !StringUtils.hasText(command.title()) || !StringUtils.hasText(command.knowledgeType())) {
            throw new BusinessException("知识文档标题和类别不能为空");
        }

        FileRecordVO fileRecord = fileService.uploadFile(
                file,
                "KNOWLEDGE_DOCUMENT",
                null,
                minioProperties.getBucket().getKnowledgeDocs(),
                "knowledge/",
                operatorId
        );

        KnowledgeDocumentEntity entity = new KnowledgeDocumentEntity();
        entity.setDocNo("KNOW" + UUID.randomUUID().toString().replace("-", ""));
        entity.setLegacyDocumentNo(entity.getDocNo());
        entity.setTitle(command.title().trim());
        entity.setFileRecordId(fileRecord.getId());
        entity.setLegacyFileId(fileRecord.getId());
        entity.setKnowledgeType(command.knowledgeType().trim());
        entity.setLegacyCategory(command.knowledgeType().trim());
        entity.setDepartmentId(command.departmentId());
        entity.setTags(command.tags());
        entity.setLegacySourceType("UPLOAD");
        entity.setAudience("PATIENT");
        entity.setVisitScope("BOTH");
        entity.setVersionNo(1);
        entity.setStatus(command.publishNow() ? "PUBLISHED" : "DRAFT");
        entity.setParserStatus("PENDING");
        entity.setChunkCount(0);
        entity.setTokenCount(0);
        entity.setPublishedAt(command.publishNow() ? LocalDateTime.now() : null);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setCreatedBy(operatorId);
        entity.setUpdatedBy(operatorId);
        entity.setLegacyUploadedBy(operatorId);
        entity.setLegacyUploadedAt(LocalDateTime.now());
        entity.setDeleted(false);
        documentService.save(entity);

        ingestService.ingestDocument(entity.getId());
        return entity;
    }

    public void publishDocument(Long documentId, Long operatorId) {
        KnowledgeDocumentEntity document = requireDocument(documentId);
        if (!"EMBEDDED".equals(document.getParserStatus()) || document.getChunkCount() == null || document.getChunkCount() <= 0) {
            throw new BusinessException("知识文档尚未完成切片入库，不能发布");
        }

        LocalDateTime now = LocalDateTime.now();
        KnowledgeDocumentEntity update = new KnowledgeDocumentEntity();
        update.setId(documentId);
        update.setStatus("PUBLISHED");
        update.setPublishedAt(now);
        update.setUpdatedAt(now);
        update.setUpdatedBy(operatorId);
        documentService.updateById(update);

        syncChunkStatus(documentId, "PUBLISHED", now);
    }

    public void offlineDocument(Long documentId, Long operatorId) {
        requireDocument(documentId);

        LocalDateTime now = LocalDateTime.now();
        KnowledgeDocumentEntity update = new KnowledgeDocumentEntity();
        update.setId(documentId);
        update.setStatus("OFFLINE");
        update.setOfflineAt(now);
        update.setUpdatedAt(now);
        update.setUpdatedBy(operatorId);
        documentService.updateById(update);

        syncChunkStatus(documentId, "OFFLINE", now);
    }

    public int reindexDocument(Long documentId, Long operatorId) {
        requireDocument(documentId);

        KnowledgeDocumentEntity update = new KnowledgeDocumentEntity();
        update.setId(documentId);
        update.setParserStatus("PENDING");
        update.setUpdatedAt(LocalDateTime.now());
        update.setUpdatedBy(operatorId);
        documentService.updateById(update);

        return ingestService.ingestDocument(documentId);
    }

    private KnowledgeDocumentEntity requireDocument(Long documentId) {
        KnowledgeDocumentEntity document = documentService.getById(documentId);
        if (document == null || Boolean.TRUE.equals(document.getDeleted())) {
            throw new BusinessException("知识文档不存在");
        }
        return document;
    }

    private void syncChunkStatus(Long documentId, String status, LocalDateTime now) {
        KnowledgeChunkEntity chunkUpdate = new KnowledgeChunkEntity();
        chunkUpdate.setDocumentStatus(status);
        chunkUpdate.setUpdatedAt(now);
        chunkService.update(
                chunkUpdate,
                new QueryWrapper<KnowledgeChunkEntity>()
                        .eq("document_id", documentId)
                        .eq("deleted", false)
        );
    }
}
