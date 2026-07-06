package com.neusoft.neu23.neuhospital.ai.dto;

import com.neusoft.neu23.neuhospital.ai.domain.entity.KnowledgeDocumentEntity;

public class KnowledgeDocumentResp {

    private Long id;
    private String docNo;
    private String title;
    private String knowledgeType;
    private Long departmentId;
    private String tags;
    private String status;
    private String parserStatus;
    private Integer chunkCount;

    public static KnowledgeDocumentResp from(KnowledgeDocumentEntity entity) {
        KnowledgeDocumentResp resp = new KnowledgeDocumentResp();
        resp.setId(entity.getId());
        resp.setDocNo(entity.getDocNo());
        resp.setTitle(entity.getTitle());
        resp.setKnowledgeType(entity.getKnowledgeType());
        resp.setDepartmentId(entity.getDepartmentId());
        resp.setTags(entity.getTags());
        resp.setStatus(entity.getStatus());
        resp.setParserStatus(entity.getParserStatus());
        resp.setChunkCount(entity.getChunkCount());
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDocNo() { return docNo; }
    public void setDocNo(String docNo) { this.docNo = docNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getKnowledgeType() { return knowledgeType; }
    public void setKnowledgeType(String knowledgeType) { this.knowledgeType = knowledgeType; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getParserStatus() { return parserStatus; }
    public void setParserStatus(String parserStatus) { this.parserStatus = parserStatus; }
    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
}
