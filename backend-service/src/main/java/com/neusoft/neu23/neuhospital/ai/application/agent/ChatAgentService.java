package com.neusoft.neu23.neuhospital.ai.application.agent;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.neu23.neuhospital.ai.domain.entity.AiChatMessageEntity;
import com.neusoft.neu23.neuhospital.ai.domain.entity.AiChatSessionEntity;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.AiChatMessageService;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.AiChatSessionService;
import com.neusoft.neu23.neuhospital.registration.entity.RegistrationEntity;
import com.neusoft.neu23.neuhospital.registration.service.RegistrationService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ChatAgentService {

    private final ChatClient agentChatClient;
    private final ChatClient analysisChatClient;
    private final AiChatSessionService sessionService;
    private final AiChatMessageService messageService;
    private final RegistrationService registrationService;
    private final RedissonClient redissonClient;
    private final PatientAgentRagOrchestrator ragOrchestrator;

    @Value("classpath:prompt/chat_agent_system.st")
    private Resource systemPromptResource;

    public ChatAgentService(@Qualifier("agentChatClient") ChatClient agentChatClient,
                            @Qualifier("analysisChatClient") ChatClient analysisChatClient,
                            AiChatSessionService sessionService,
                            AiChatMessageService messageService,
                            RegistrationService registrationService,
                            RedissonClient redissonClient,
                            PatientAgentRagOrchestrator ragOrchestrator) {
        this.agentChatClient = agentChatClient;
        this.analysisChatClient = analysisChatClient;
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.registrationService = registrationService;
        this.redissonClient = redissonClient;
        this.ragOrchestrator = ragOrchestrator;
    }

    public AiChatSessionEntity createSession(Long patientId, Long registrationId, String sessionType) {
        validateRegistrationOwnership(patientId, registrationId);

        AiChatSessionEntity session = new AiChatSessionEntity();
        session.setSessionNo("CHAT" + UUID.randomUUID().toString().replace("-", ""));
        session.setPatientId(patientId);
        session.setRegistrationId(registrationId);
        session.setSessionType(sessionType);
        session.setStatus("ENABLED");
        session.setStartedAt(LocalDateTime.now());
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        session.setDeleted(false);
        sessionService.save(session);
        return session;
    }

    public String chat(String sessionNo, Long currentPatientId, String userMessage) {
        AiChatSessionEntity session = sessionService.getOne(new QueryWrapper<AiChatSessionEntity>()
                .eq("session_no", sessionNo)
                .eq("patient_id", currentPatientId)
                .eq("status", "ENABLED")
                .eq("deleted", false)
                .last("LIMIT 1"));
        if (session == null) {
            throw new IllegalArgumentException("会话不存在或无权访问");
        }

        Long sessionId = session.getId();
        AiAgentSessionContext.bind(sessionId, session.getPatientId(), session.getRegistrationId());

        RLock lock = redissonClient.getLock("medical:ai:chat:lock:" + sessionId);
        boolean isLocked = false;
        try {
            isLocked = lock.tryLock(2, 30, TimeUnit.SECONDS);
            if (!isLocked) {
                return "AI 正在思考中，请稍后再发送消息...";
            }

            AiChatMessageEntity userMsgEntity = new AiChatMessageEntity();
            userMsgEntity.setSessionId(sessionId);
            userMsgEntity.setMessageRole("USER");
            userMsgEntity.setMessageContent(userMessage);
            userMsgEntity.setSequenceNo(getSequenceNo(sessionId));
            userMsgEntity.setCreatedAt(LocalDateTime.now());
            messageService.save(userMsgEntity);

            Integer lastSeq = session.getLastSummarizedSeq() == null ? 0 : session.getLastSummarizedSeq();
            List<AiChatMessageEntity> unsummarizedHistory = messageService.list(new QueryWrapper<AiChatMessageEntity>()
                    .eq("session_id", sessionId)
                    .gt("sequence_no", lastSeq)
                    .orderByAsc("sequence_no"));

            PatientAgentRagContext ragContext = ragOrchestrator.prepareContext(
                    userMessage,
                    session.getSummary(),
                    unsummarizedHistory
            );

            List<Message> messages = buildPromptMessages(session, unsummarizedHistory, ragContext);

            String responseContent = agentChatClient.prompt()
                    .messages(messages)
                    .call()
                    .content();

            AiChatMessageEntity assistantMsgEntity = new AiChatMessageEntity();
            assistantMsgEntity.setSessionId(sessionId);
            assistantMsgEntity.setMessageRole("ASSISTANT");
            assistantMsgEntity.setMessageContent(responseContent);
            assistantMsgEntity.setSequenceNo(getSequenceNo(sessionId));
            assistantMsgEntity.setCreatedAt(LocalDateTime.now());
            messageService.save(assistantMsgEntity);

            if (unsummarizedHistory.size() + 1 >= 10) {
                triggerAsyncSummarization(session, unsummarizedHistory, assistantMsgEntity);
            }

            return responseContent;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("等待 AI 响应超时", e);
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            AiAgentSessionContext.clear();
        }
    }

    private List<Message> buildPromptMessages(AiChatSessionEntity session,
                                              List<AiChatMessageEntity> unsummarizedHistory,
                                              PatientAgentRagContext ragContext) {
        List<Message> messages = new ArrayList<>();

        SystemPromptTemplate promptTemplate = new SystemPromptTemplate(systemPromptResource);
        Message systemMessage = promptTemplate.createMessage(Map.of("patientId", session.getPatientId()));
        messages.add(systemMessage);

        if (session.getSummary() != null && !session.getSummary().trim().isEmpty()) {
            messages.add(new SystemMessage("历史前情摘要：" + session.getSummary()));
        }

        messages.add(new SystemMessage(ragContext.evidenceSystemMessage()));

        for (AiChatMessageEntity msg : unsummarizedHistory) {
            if ("USER".equals(msg.getMessageRole())) {
                messages.add(new UserMessage(msg.getMessageContent()));
            } else if ("ASSISTANT".equals(msg.getMessageRole())) {
                messages.add(new AssistantMessage(msg.getMessageContent()));
            }
        }
        return messages;
    }

    private Integer getSequenceNo(Long sessionId) {
        long count = messageService.count(new QueryWrapper<AiChatMessageEntity>().eq("session_id", sessionId));
        return (int) (count + 1);
    }

    private void validateRegistrationOwnership(Long patientId, Long registrationId) {
        if (registrationId == null) {
            return;
        }

        RegistrationEntity registration = registrationService.getRegistrationById(registrationId);
        if (registration == null || registration.getPatientId() == null || !registration.getPatientId().equals(patientId)) {
            throw new IllegalArgumentException("挂号记录不存在或无权使用");
        }
    }

    private void triggerAsyncSummarization(AiChatSessionEntity session, List<AiChatMessageEntity> unsummarizedHistory, AiChatMessageEntity lastAssistantMsg) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                if (session.getSummary() != null && !session.getSummary().isEmpty()) {
                    sb.append("这是之前提炼的历史问诊摘要：\n").append(session.getSummary()).append("\n\n");
                }
                sb.append("以下是需要你继续压缩合并的最新几轮对话：\n");
                for (AiChatMessageEntity msg : unsummarizedHistory) {
                    sb.append(msg.getMessageRole()).append(": ").append(msg.getMessageContent()).append("\n");
                }
                sb.append("ASSISTANT: ").append(lastAssistantMsg.getMessageContent()).append("\n\n");
                sb.append("请你扮演总结助手，将以上旧摘要与最新对话内容合并，提炼出一份全新的、囊括所有关键医疗体征、症状和上下文逻辑的《问诊摘要》。只输出摘要内容本身，不要输出其他客套话。");

                String newSummary = analysisChatClient.prompt()
                        .user(sb.toString())
                        .call()
                        .content();

                session.setSummary(newSummary);
                session.setLastSummarizedSeq(lastAssistantMsg.getSequenceNo());
                sessionService.updateById(session);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
