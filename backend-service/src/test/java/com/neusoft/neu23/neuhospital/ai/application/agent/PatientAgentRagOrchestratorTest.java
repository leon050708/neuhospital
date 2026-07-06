package com.neusoft.neu23.neuhospital.ai.application.agent;

import com.neusoft.neu23.neuhospital.ai.application.rag.KnowledgeSearchHit;
import com.neusoft.neu23.neuhospital.ai.application.rag.KnowledgeSearchRequest;
import com.neusoft.neu23.neuhospital.ai.application.rag.KnowledgeSearchService;
import com.neusoft.neu23.neuhospital.ai.domain.entity.AiChatMessageEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientAgentRagOrchestratorTest {

    @Test
    void shouldSearchWithRewrittenQueriesAndBuildEvidenceMessage() {
        PatientAgentLlmSupportService llmSupportService = mock(PatientAgentLlmSupportService.class);
        KnowledgeSearchService knowledgeSearchService = mock(KnowledgeSearchService.class);

        PatientAgentRagOrchestrator orchestrator = new PatientAgentRagOrchestrator(llmSupportService, knowledgeSearchService);

        AiChatMessageEntity recentUser = new AiChatMessageEntity();
        recentUser.setMessageRole("USER");
        recentUser.setMessageContent("我是初诊。");

        PatientAgentRoutingDecision decision = new PatientAgentRoutingDecision(
                true,
                false,
                true,
                "AMBIGUOUS",
                List.of("REGISTRATION_PROCESS", "VISIT_PREPARATION"),
                List.of(),
                "初诊",
                List.of()
        );

        when(llmSupportService.route(any(PatientAgentContextWindow.class))).thenReturn(decision);
        when(llmSupportService.rewriteQueries(any(PatientAgentContextWindow.class), any(PatientAgentRoutingDecision.class)))
                .thenReturn(List.of("初诊挂号流程", "初诊需要准备什么材料"));

        KnowledgeSearchHit hit1 = new KnowledgeSearchHit(1L, 10L, "初诊患者挂号需要先建档。", "REGISTRATION_PROCESS", null, "初诊,挂号", 0.81);
        KnowledgeSearchHit hit2 = new KnowledgeSearchHit(2L, 10L, "初诊请携带身份证与医保卡。", "VISIT_PREPARATION", null, "初诊,材料", 0.79);

        when(knowledgeSearchService.search(any(KnowledgeSearchRequest.class)))
                .thenReturn(List.of(hit1), List.of(hit2));
        when(llmSupportService.rerank(anyString(), anyList())).thenReturn(List.of(hit2, hit1));

        PatientAgentRagContext context = orchestrator.prepareContext(
                "那我第一次来要怎么挂号，要带什么？",
                "患者在咨询初诊流程",
                List.of(recentUser)
        );

        assertTrue(context.hasEvidence());
        assertEquals(2, context.evidenceHits().size());
        assertEquals(2L, context.evidenceHits().get(0).chunkId());
        assertTrue(context.evidenceSystemMessage().contains("医院知识证据"));
        assertTrue(context.evidenceSystemMessage().contains("工具结果"));
        assertTrue(context.evidenceSystemMessage().contains("初诊请携带身份证与医保卡"));

        ArgumentCaptor<KnowledgeSearchRequest> captor = ArgumentCaptor.forClass(KnowledgeSearchRequest.class);
        verify(knowledgeSearchService, org.mockito.Mockito.times(2)).search(captor.capture());
        List<KnowledgeSearchRequest> requests = captor.getAllValues();
        assertEquals("初诊挂号流程", requests.get(0).query());
        assertEquals(6, requests.get(0).topK());
        assertEquals(0.55, requests.get(0).minScore(), 0.0001);
    }

    @Test
    void shouldSkipRetrievalWhenRouterSaysNo() {
        PatientAgentLlmSupportService llmSupportService = mock(PatientAgentLlmSupportService.class);
        KnowledgeSearchService knowledgeSearchService = mock(KnowledgeSearchService.class);

        PatientAgentRagOrchestrator orchestrator = new PatientAgentRagOrchestrator(llmSupportService, knowledgeSearchService);

        when(llmSupportService.route(any(PatientAgentContextWindow.class))).thenReturn(new PatientAgentRoutingDecision(
                false,
                false,
                false,
                "CLEAR",
                List.of(),
                List.of(),
                null,
                List.of()
        ));

        PatientAgentRagContext context = orchestrator.prepareContext("我最近头疼是怎么回事", "", List.of());

        assertTrue(context.evidenceHits().isEmpty());
        assertTrue(context.evidenceSystemMessage().contains("本轮未检索"));
    }
}