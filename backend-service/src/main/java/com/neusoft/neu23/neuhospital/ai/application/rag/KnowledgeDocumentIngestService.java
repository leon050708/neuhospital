package com.neusoft.neu23.neuhospital.ai.application.rag;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.neu23.neuhospital.ai.domain.entity.KnowledgeChunkEntity;
import com.neusoft.neu23.neuhospital.ai.domain.entity.KnowledgeDocumentEntity;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.KnowledgeChunkService;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.KnowledgeDocumentService;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.file.service.FileService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class KnowledgeDocumentIngestService {

    private final KnowledgeDocumentService documentService;
    private final KnowledgeChunkService chunkService;
    private final FileService fileService;
    private final KnowledgeDocumentTextExtractor extractor;
    private final KnowledgeTextCleaner cleaner;
    private final KnowledgeChunker chunker;
    private final EmbeddingModel embeddingModel;
    private final String embeddingModelName;

    public KnowledgeDocumentIngestService(KnowledgeDocumentService documentService,
                                          KnowledgeChunkService chunkService,
                                          FileService fileService,
                                          KnowledgeDocumentTextExtractor extractor,
                                          KnowledgeTextCleaner cleaner,
                                          KnowledgeChunker chunker,
                                          EmbeddingModel embeddingModel,
                                          @Value("${spring.ai.openai.embedding.options.model:text-embedding-v3}") String embeddingModelName) {
        this.documentService = documentService;
        this.chunkService = chunkService;
        this.fileService = fileService;
        this.extractor = extractor;
        this.cleaner = cleaner;
        this.chunker = chunker;
        this.embeddingModel = embeddingModel;
        this.embeddingModelName = embeddingModelName;
    }

    public int ingestDocument(Long documentId) {
        KnowledgeDocumentEntity document = documentService.getById(documentId);
        if (document == null || Boolean.TRUE.equals(document.getDeleted())) {
            throw new BusinessException("知识文档不存在");
        }
        if (document.getFileRecordId() == null) {
            throw new BusinessException("知识文档缺少原始文件");
        }

        updateDocumentState(document, "RUNNING", null, null);

        FileService.DownloadFile downloadFile = fileService.downloadFile(document.getFileRecordId());
        try (var inputStream = downloadFile.inputStream()) {
            ExtractedKnowledgeText extracted = extractor.extract(downloadFile.fileRecord(), inputStream);
            String cleanedText = cleaner.clean(extracted.text());
            List<String> chunks = chunker.chunk(cleanedText);

            chunkService.remove(new QueryWrapper<KnowledgeChunkEntity>()
                    .eq("document_id", document.getId()));

            List<KnowledgeChunkEntity> entities = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                KnowledgeChunkEntity entity = new KnowledgeChunkEntity();
                entity.setDocumentId(document.getId());
                entity.setChunkNo(i + 1);
                entity.setContentText(chunkText);
                entity.setLegacyChunkText(chunkText);
                entity.setCharCount(chunkText.length());
                entity.setTokenCount(chunkText.length());
                entity.setKnowledgeType(document.getKnowledgeType());
                entity.setDepartmentId(document.getDepartmentId());
                entity.setTags(document.getTags());
                entity.setDocumentStatus(document.getStatus());
                entity.setEmbeddingText(toVectorLiteral(embeddingModel.embed(chunkText)));
                entity.setEmbeddingModel(embeddingModelName);
                entity.setEmbeddingVersion(1);
                entity.setCreatedAt(LocalDateTime.now());
                entity.setUpdatedAt(LocalDateTime.now());
                entity.setDeleted(false);
                entities.add(entity);
            }
            chunkService.saveBatch(entities);
            updateDocumentState(document, "EMBEDDED", chunks.size(), cleanedText.length());
            return chunks.size();
        } catch (Exception ex) {
            updateDocumentState(document, "FAILED", 0, 0);
            throw new BusinessException("知识文档入库失败: " + ex.getMessage());
        }
    }

    private void updateDocumentState(KnowledgeDocumentEntity document, String parserStatus, Integer chunkCount, Integer tokenCount) {
        KnowledgeDocumentEntity update = new KnowledgeDocumentEntity();
        update.setId(document.getId());
        update.setParserStatus(parserStatus);
        if (chunkCount != null) {
            update.setChunkCount(chunkCount);
        }
        if (tokenCount != null) {
            update.setTokenCount(tokenCount);
        }
        update.setLastIndexedAt(LocalDateTime.now());
        update.setUpdatedAt(LocalDateTime.now());
        documentService.updateById(update);
    }

    private String toVectorLiteral(float[] embedding) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(String.format(Locale.ROOT, "%.6f", embedding[i]));
        }
        builder.append(']');
        return builder.toString();
    }
}
