package com.neusoft.neu23.neuhospital.ai.application.agent;

import com.neusoft.neu23.neuhospital.ai.application.rag.KnowledgeSearchHit;

import java.util.List;

public record PatientAgentRagContext(
        PatientAgentRoutingDecision routingDecision,
        List<KnowledgeSearchHit> evidenceHits,
        String evidenceSystemMessage
) {

    public PatientAgentRagContext {
        evidenceHits = evidenceHits == null ? List.of() : List.copyOf(evidenceHits);
        evidenceSystemMessage = evidenceSystemMessage == null ? "" : evidenceSystemMessage;
    }

    public boolean hasEvidence() {
        return !evidenceHits.isEmpty();
    }
}
