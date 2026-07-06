package com.neusoft.neu23.neuhospital.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("knowledge_chunk")
public class KnowledgeChunkEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long documentId;
    private Integer chunkNo;
    private String sectionPath;
    private String sectionTitle;
    private String contentText;
    @TableField("chunk_text")
    private String legacyChunkText;
    private String contentHash;
    private Integer charCount;
    private Integer tokenCount;
    private String knowledgeType;
    private Long departmentId;
    private String tags;
    private String documentStatus;
    private String sourcePage;
    private Integer sourceOrder;
    @TableField("embedding")
    private String embeddingText;
    private String embeddingModel;
    private Integer embeddingVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public Integer getChunkNo() { return chunkNo; }
    public void setChunkNo(Integer chunkNo) { this.chunkNo = chunkNo; }
    public String getSectionPath() { return sectionPath; }
    public void setSectionPath(String sectionPath) { this.sectionPath = sectionPath; }
    public String getSectionTitle() { return sectionTitle; }
    public void setSectionTitle(String sectionTitle) { this.sectionTitle = sectionTitle; }
    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }
    public String getLegacyChunkText() { return legacyChunkText; }
    public void setLegacyChunkText(String legacyChunkText) { this.legacyChunkText = legacyChunkText; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public Integer getCharCount() { return charCount; }
    public void setCharCount(Integer charCount) { this.charCount = charCount; }
    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
    public String getKnowledgeType() { return knowledgeType; }
    public void setKnowledgeType(String knowledgeType) { this.knowledgeType = knowledgeType; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getDocumentStatus() { return documentStatus; }
    public void setDocumentStatus(String documentStatus) { this.documentStatus = documentStatus; }
    public String getSourcePage() { return sourcePage; }
    public void setSourcePage(String sourcePage) { this.sourcePage = sourcePage; }
    public Integer getSourceOrder() { return sourceOrder; }
    public void setSourceOrder(Integer sourceOrder) { this.sourceOrder = sourceOrder; }
    public String getEmbeddingText() { return embeddingText; }
    public void setEmbeddingText(String embeddingText) { this.embeddingText = embeddingText; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    public Integer getEmbeddingVersion() { return embeddingVersion; }
    public void setEmbeddingVersion(Integer embeddingVersion) { this.embeddingVersion = embeddingVersion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
}
