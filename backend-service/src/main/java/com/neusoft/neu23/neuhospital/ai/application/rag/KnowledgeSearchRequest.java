package com.neusoft.neu23.neuhospital.ai.application.rag;

import java.util.List;

public record KnowledgeSearchRequest(
        String query,
        List<String> knowledgeTypes,
        Long departmentId,
        String tagKeyword,
        int topK,
        double minScore
) {
}
