package com.neusoft.neu23.neuhospital.ai.application.rag;

import com.neusoft.neu23.neuhospital.ai.domain.entity.KnowledgeDocumentEntity;
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
    private final KnowledgeDocumentIngestService ingestService;
    private final MinioProperties minioProperties;

    public KnowledgeAdminService(FileService fileService,
                                 KnowledgeDocumentService documentService,
                                 KnowledgeDocumentIngestService ingestService,
                                 MinioProperties minioProperties) {
        this.fileService = fileService;
        this.documentService = documentService;
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
        KnowledgeDocumentEntity update = new KnowledgeDocumentEntity();
        update.setId(documentId);
        update.setStatus("PUBLISHED");
        update.setPublishedAt(LocalDateTime.now());
        update.setUpdatedAt(LocalDateTime.now());
        update.setUpdatedBy(operatorId);
        documentService.updateById(update);
    }

    public void offlineDocument(Long documentId, Long operatorId) {
        KnowledgeDocumentEntity update = new KnowledgeDocumentEntity();
        update.setId(documentId);
        update.setStatus("OFFLINE");
        update.setOfflineAt(LocalDateTime.now());
        update.setUpdatedAt(LocalDateTime.now());
        update.setUpdatedBy(operatorId);
        documentService.updateById(update);
    }
}
