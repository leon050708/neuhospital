package com.neusoft.neu23.neuhospital.ai.application.rag;

public record KnowledgeDocumentUploadCommand(
        String title,
        String knowledgeType,
        Long departmentId,
        String tags,
        boolean publishNow
) {
}
