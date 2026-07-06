package com.neusoft.neu23.neuhospital.ai.application.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.neu23.neuhospital.ai.application.rag.KnowledgeSearchHit;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PatientAgentLlmSupportService {

    private final ChatClient analysisChatClient;
    private final ObjectMapper objectMapper;

    public PatientAgentLlmSupportService(@Qualifier("analysisChatClient") ChatClient analysisChatClient,
                                         ObjectMapper objectMapper) {
        this.analysisChatClient = analysisChatClient;
        this.objectMapper = objectMapper;
    }

    public PatientAgentRoutingDecision route(PatientAgentContextWindow contextWindow) {
        String systemPrompt = """
                你是医院患者助诊 Agent 的轻量路由器。请根据对话上下文判断：
                1. 当前问题是否需要检索医院知识库
                2. 是否需要后续结合工具处理
                3. 是否需要改写检索问题
                4. 当前问题清晰度：CLEAR / NORMAL / AMBIGUOUS
                5. 知识分类 knowledgeTypes，可选值：
                   REGISTRATION_PROCESS, VISIT_NOTICE, DEPARTMENT_INFO, VISIT_PREPARATION, EXAM_NOTICE, FAQ
                6. 工具候选 toolCandidates，可选值：
                   getPatientInfo, updatePatientMemory, queryDepartment, querySchedule, bookRegistration
                7. 可用于 metadata 粗过滤的 tagKeyword
                8. 如果问题已经足够明确，可直接给 searchQueries；否则留空

                只输出 JSON，不要输出解释。JSON 结构固定如下：
                {
                  \"retrievalNeeded\": true,
                  \"toolNeeded\": false,
                  \"rewriteNeeded\": true,
                  \"clarity\": \"AMBIGUOUS\",
                  \"knowledgeTypes\": [\"REGISTRATION_PROCESS\"],
                  \"toolCandidates\": [\"queryDepartment\"],
                  \"tagKeyword\": \"挂号\",
                  \"searchQueries\": [\"初诊挂号流程\"]
                }
                """;

        try {
            String content = complete(List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(contextWindow.renderForPrompt())
            ));
            JsonNode root = objectMapper.readTree(extractJson(content));
            return new PatientAgentRoutingDecision(
                    root.path("retrievalNeeded").asBoolean(false),
                    root.path("toolNeeded").asBoolean(false),
                    root.path("rewriteNeeded").asBoolean(false),
                    root.path("clarity").asText("NORMAL"),
                    readStringList(root.path("knowledgeTypes")),
                    readStringList(root.path("toolCandidates")),
                    readNullableText(root.path("tagKeyword")),
                    readStringList(root.path("searchQueries"))
            );
        } catch (Exception ex) {
            return fallbackRoute(contextWindow.currentMessage());
        }
    }

    public List<String> rewriteQueries(PatientAgentContextWindow contextWindow, PatientAgentRoutingDecision decision) {
        if (!decision.rewriteNeeded()) {
            return normalizeQueries(decision.searchQueries(), contextWindow.currentMessage());
        }

        String systemPrompt = """
                你是医院知识库检索改写助手。
                请结合历史摘要、最近对话和当前问题，把口语化问法改写成适合检索医院手册/流程文档的问题。
                要求：
                1. 只保留和医院知识库有关的流程/科室/就诊须知/材料/检查准备信息
                2. 如当前问题同时包含多个意图，可拆成 2 到 3 个 queries
                3. 输出 JSON：{\"queries\":[\"...\",\"...\"]}
                4. 不要输出任何解释
                """;

        try {
            String content = complete(List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(contextWindow.renderForPrompt())
            ));
            JsonNode root = objectMapper.readTree(extractJson(content));
            return normalizeQueries(readStringList(root.path("queries")), contextWindow.currentMessage());
        } catch (Exception ex) {
            return fallbackRewrite(contextWindow);
        }
    }

    public List<KnowledgeSearchHit> rerank(String userMessage, List<KnowledgeSearchHit> candidateHits) {
        if (candidateHits == null || candidateHits.size() <= 1) {
            return candidateHits == null ? List.of() : List.copyOf(candidateHits);
        }

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("用户问题:\n").append(userMessage).append("\n\n候选证据:\n");
        for (KnowledgeSearchHit hit : candidateHits) {
            userPrompt.append("chunkId=").append(hit.chunkId())
                    .append(", knowledgeType=").append(hit.knowledgeType())
                    .append(", tags=").append(hit.tags())
                    .append(", text=").append(hit.contentText())
                    .append("\n");
        }

        String systemPrompt = """
                你是医院知识证据重排助手。请根据用户问题，对候选 chunk 按相关性从高到低排序。
                只输出 JSON：{\"chunkIds\":[2,1,3]}
                不要输出解释。
                """;

        try {
            String content = complete(List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(userPrompt.toString())
            ));
            JsonNode root = objectMapper.readTree(extractJson(content));
            List<Long> orderedIds = new ArrayList<>();
            for (JsonNode item : root.path("chunkIds")) {
                if (item.canConvertToLong()) {
                    orderedIds.add(item.asLong());
                }
            }
            if (orderedIds.isEmpty()) {
                return List.copyOf(candidateHits);
            }

            Map<Long, KnowledgeSearchHit> index = new LinkedHashMap<>();
            for (KnowledgeSearchHit hit : candidateHits) {
                index.put(hit.chunkId(), hit);
            }

            List<KnowledgeSearchHit> ranked = new ArrayList<>();
            for (Long id : orderedIds) {
                KnowledgeSearchHit hit = index.remove(id);
                if (hit != null) {
                    ranked.add(hit);
                }
            }
            ranked.addAll(index.values());
            return ranked;
        } catch (Exception ex) {
            return List.copyOf(candidateHits);
        }
    }

    private String complete(List<Message> messages) {
        return analysisChatClient.prompt().messages(messages).call().content();
    }

    private PatientAgentRoutingDecision fallbackRoute(String currentMessage) {
        String normalized = currentMessage == null ? "" : currentMessage.trim();
        boolean toolNeeded = containsAny(normalized, "挂号", "预约", "排班", "号源", "挂哪个科", "看什么科");
        boolean retrievalNeeded = toolNeeded || containsAny(normalized, "流程", "须知", "材料", "初诊", "复诊", "检查", "注意事项", "门诊", "科室", "怎么挂");
        boolean rewriteNeeded = retrievalNeeded && (normalized.length() <= 12 || containsAny(normalized, "这个", "那个", "怎么办", "怎么弄"));

        return new PatientAgentRoutingDecision(
                retrievalNeeded,
                toolNeeded,
                rewriteNeeded,
                inferClarity(normalized),
                inferKnowledgeTypes(normalized),
                inferToolCandidates(normalized),
                inferTagKeyword(normalized),
                List.of()
        );
    }

    private List<String> fallbackRewrite(PatientAgentContextWindow contextWindow) {
        String currentMessage = contextWindow.currentMessage() == null ? "" : contextWindow.currentMessage().trim();
        if (containsAny(currentMessage, "挂号", "预约") && containsAny(currentMessage, "材料", "准备", "带什么")) {
            return List.of("挂号流程", "初诊就诊准备材料");
        }
        return List.of(currentMessage);
    }

    private String inferClarity(String message) {
        if (message == null || message.isBlank()) {
            return "AMBIGUOUS";
        }
        if (message.length() <= 8 || containsAny(message, "这个", "那个", "怎么办")) {
            return "AMBIGUOUS";
        }
        if (message.length() >= 25) {
            return "CLEAR";
        }
        return "NORMAL";
    }

    private List<String> inferKnowledgeTypes(String message) {
        List<String> types = new ArrayList<>();
        if (containsAny(message, "挂号", "预约", "排班", "号源", "流程")) {
            types.add("REGISTRATION_PROCESS");
        }
        if (containsAny(message, "科室", "门诊", "挂哪个科", "看什么科")) {
            types.add("DEPARTMENT_INFO");
        }
        if (containsAny(message, "初诊", "复诊", "材料", "身份证", "医保")) {
            types.add("VISIT_PREPARATION");
        }
        if (containsAny(message, "检查", "空腹", "注意事项")) {
            types.add("EXAM_NOTICE");
        }
        if (containsAny(message, "须知", "FAQ", "常见问题")) {
            types.add("VISIT_NOTICE");
        }
        return types.isEmpty() ? List.of("FAQ") : types;
    }

    private List<String> inferToolCandidates(String message) {
        List<String> tools = new ArrayList<>();
        if (containsAny(message, "挂哪个科", "看什么科", "科室")) {
            tools.add("queryDepartment");
        }
        if (containsAny(message, "排班", "号源", "医生")) {
            tools.add("querySchedule");
        }
        if (containsAny(message, "预约排班ID", "我要预约", "帮我挂号")) {
            tools.add("bookRegistration");
        }
        return tools;
    }

    private String inferTagKeyword(String message) {
        if (containsAny(message, "初诊")) {
            return "初诊";
        }
        if (containsAny(message, "复诊")) {
            return "复诊";
        }
        if (containsAny(message, "挂号", "预约")) {
            return "挂号";
        }
        if (containsAny(message, "检查")) {
            return "检查";
        }
        return null;
    }

    private List<String> normalizeQueries(List<String> queries, String fallback) {
        List<String> normalized = new ArrayList<>();
        for (String query : queries) {
            if (query != null && !query.isBlank()) {
                normalized.add(query.trim());
            }
        }
        if (normalized.isEmpty() && fallback != null && !fallback.isBlank()) {
            normalized.add(fallback.trim());
        }
        return normalized;
    }

    private List<String> readStringList(JsonNode jsonNode) {
        if (jsonNode == null || !jsonNode.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        jsonNode.forEach(node -> {
            String text = node.asText(null);
            if (text != null && !text.isBlank()) {
                values.add(text.trim());
            }
        });
        return values;
    }

    private String readNullableText(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return null;
        }
        String text = jsonNode.asText(null);
        return text == null || text.isBlank() ? null : text.trim();
    }

    private String extractJson(String raw) {
        if (raw == null) {
            return "{}";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstBrace = trimmed.indexOf('{');
            int lastBrace = trimmed.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                return trimmed.substring(firstBrace, lastBrace + 1);
            }
        }
        return trimmed;
    }

    private boolean containsAny(String text, String... keywords) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
