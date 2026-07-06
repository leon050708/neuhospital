package com.neusoft.neu23.neuhospital.ai.application.rag;

public record KnowledgeSearchHit(
        Long chunkId,
        Long documentId,
        String contentText,
        String knowledgeType,
        Long departmentId,
        String tags,
        double score
) {
}
