package com.neusoft.neu23.neuhospital.ai.application.agent;

import java.util.List;

public record PatientAgentRoutingDecision(
        boolean retrievalNeeded,
        boolean toolNeeded,
        boolean rewriteNeeded,
        String clarity,
        List<String> knowledgeTypes,
        List<String> toolCandidates,
        String tagKeyword,
        List<String> searchQueries
) {

    public PatientAgentRoutingDecision {
        knowledgeTypes = knowledgeTypes == null ? List.of() : List.copyOf(knowledgeTypes);
        toolCandidates = toolCandidates == null ? List.of() : List.copyOf(toolCandidates);
        searchQueries = searchQueries == null ? List.of() : List.copyOf(searchQueries);
        clarity = clarity == null || clarity.isBlank() ? "NORMAL" : clarity;
    }
}
