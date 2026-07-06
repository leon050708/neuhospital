package com.neusoft.neu23.neuhospital.ai.application.agent;

import com.neusoft.neu23.neuhospital.ai.application.rag.KnowledgeSearchHit;
import com.neusoft.neu23.neuhospital.ai.application.rag.KnowledgeSearchRequest;
import com.neusoft.neu23.neuhospital.ai.application.rag.KnowledgeSearchService;
import com.neusoft.neu23.neuhospital.ai.domain.entity.AiChatMessageEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PatientAgentRagOrchestrator {

    private final PatientAgentLlmSupportService llmSupportService;
    private final KnowledgeSearchService knowledgeSearchService;

    public PatientAgentRagOrchestrator(PatientAgentLlmSupportService llmSupportService,
                                       KnowledgeSearchService knowledgeSearchService) {
        this.llmSupportService = llmSupportService;
        this.knowledgeSearchService = knowledgeSearchService;
    }

    public PatientAgentRagContext prepareContext(String userMessage,
                                                 String sessionSummary,
                                                 List<AiChatMessageEntity> recentMessages) {
        PatientAgentContextWindow contextWindow = new PatientAgentContextWindow(userMessage, sessionSummary, recentMessages);
        PatientAgentRoutingDecision decision = llmSupportService.route(contextWindow);

        if (!decision.retrievalNeeded()) {
            return new PatientAgentRagContext(decision, List.of(), buildEmptyEvidenceMessage(decision));
        }

        SearchTuning tuning = resolveSearchTuning(decision.clarity());
        List<String> queries = decision.searchQueries().isEmpty()
                ? llmSupportService.rewriteQueries(contextWindow, decision)
                : decision.searchQueries();

        Map<Long, KnowledgeSearchHit> deduplicated = new LinkedHashMap<>();
        for (String query : queries) {
            KnowledgeSearchRequest request = new KnowledgeSearchRequest(
                    query,
                    decision.knowledgeTypes(),
                    null,
                    decision.tagKeyword(),
                    tuning.topK(),
                    tuning.minScore()
            );
            for (KnowledgeSearchHit hit : knowledgeSearchService.search(request)) {
                deduplicated.merge(hit.chunkId(), hit, this::pickHigherScore);
            }
        }

        List<KnowledgeSearchHit> mergedHits = new ArrayList<>(deduplicated.values());
        mergedHits.sort(Comparator.comparingDouble(KnowledgeSearchHit::score).reversed());
        if (mergedHits.size() > tuning.rerankSize()) {
            mergedHits = new ArrayList<>(mergedHits.subList(0, tuning.rerankSize()));
        }
        List<KnowledgeSearchHit> reranked = llmSupportService.rerank(userMessage, mergedHits);
        List<KnowledgeSearchHit> finalHits = reranked.size() > 4 ? reranked.subList(0, 4) : reranked;

        return new PatientAgentRagContext(decision, finalHits, buildEvidenceSystemMessage(finalHits, decision));
    }

    private KnowledgeSearchHit pickHigherScore(KnowledgeSearchHit left, KnowledgeSearchHit right) {
        return left.score() >= right.score() ? left : right;
    }

    private SearchTuning resolveSearchTuning(String clarity) {
        if ("AMBIGUOUS".equalsIgnoreCase(clarity)) {
            return new SearchTuning(6, 0.55, 6);
        }
        if ("CLEAR".equalsIgnoreCase(clarity)) {
            return new SearchTuning(4, 0.68, 5);
        }
        return new SearchTuning(5, 0.62, 5);
    }

    private String buildEmptyEvidenceMessage(PatientAgentRoutingDecision decision) {
        return """
                [医院知识证据]
                本轮未检索医院知识库，原因：当前问题更偏向通用问诊交流，或暂不需要医院流程证据。

                [回答约束]
                1. 如果你后续使用工具，请以工具结果为准。
                2. 如果你要陈述“本院流程/规定/就诊须知”，但当前没有医院证据，请明确说“我暂时没有在医院知识库中查到这一条院内规定”，不要把通用常识说成医院规定。
                3. 可以提供一般性医学建议，但必须明确这是通用建议，不代表本院规定。
                """;
    }

    private String buildEvidenceSystemMessage(List<KnowledgeSearchHit> hits, PatientAgentRoutingDecision decision) {
        StringBuilder builder = new StringBuilder();
        builder.append("[医院知识证据]\n");
        if (hits.isEmpty()) {
            builder.append("本轮已尝试检索，但没有找到足够可信的医院知识证据。\n");
        } else {
            int index = 1;
            for (KnowledgeSearchHit hit : hits) {
                builder.append(index++)
                        .append(". knowledgeType=")
                        .append(hit.knowledgeType())
                        .append(", score=")
                        .append(String.format("%.3f", hit.score()))
                        .append(", tags=")
                        .append(hit.tags())
                        .append("\n")
                        .append(hit.contentText())
                        .append("\n\n");
            }
        }
        builder.append("[回答约束]\n")
                .append("1. 回答优先级：工具结果 > 医院知识证据 > 通用医学常识。\n")
                .append("2. 只要涉及院内挂号流程、科室说明、就诊须知、检查准备，就优先依据上面的医院知识证据回答。\n")
                .append("3. 如果证据不足，请明确说“我暂时没有在医院知识库中查到足够依据”，不要臆造院内规定。\n")
                .append("4. 通用医学常识可以补充，但必须明确标注为一般建议，不能说成“本院要求”。\n");

        if (decision.toolNeeded()) {
            builder.append("5. 当前问题可能还需要结合工具结果，请在需要时主动调用工具，不要伪造实时数据。\n");
        }
        return builder.toString();
    }

    private record SearchTuning(int topK, double minScore, int rerankSize) {
    }
}
