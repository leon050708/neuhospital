package com.neusoft.neu23.neuhospital.ai.application.agent;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.neu23.neuhospital.ai.domain.entity.AiChatMessageEntity;
import com.neusoft.neu23.neuhospital.ai.domain.entity.AiChatSessionEntity;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.AiChatMessageService;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.AiChatSessionService;
import com.neusoft.neu23.neuhospital.registration.entity.RegistrationEntity;
import com.neusoft.neu23.neuhospital.registration.service.RegistrationService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ChatAgentService {

    private final ChatClient chatClient;
    private final AiChatSessionService sessionService;
    private final AiChatMessageService messageService;
    private final RegistrationService registrationService;

    @Value("classpath:prompt/chat_agent_system.st")
    private Resource systemPromptResource;

    public ChatAgentService(ChatClient.Builder chatClientBuilder,
                            AiChatSessionService sessionService,
                            AiChatMessageService messageService,
                            RegistrationService registrationService) {
        this.chatClient = chatClientBuilder
                .defaultFunctions("getPatientInfo", "updatePatientMemory", "queryDepartment", "querySchedule", "bookRegistration")
                .build();
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.registrationService = registrationService;
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
        try {
            // Save User Message
            AiChatMessageEntity userMsgEntity = new AiChatMessageEntity();
            userMsgEntity.setSessionId(sessionId);
            userMsgEntity.setMessageRole("USER");
            userMsgEntity.setMessageContent(userMessage);
            userMsgEntity.setSequenceNo(getSequenceNo(sessionId));
            userMsgEntity.setCreatedAt(LocalDateTime.now());
            messageService.save(userMsgEntity);

            // Fetch History
            List<AiChatMessageEntity> history = messageService.list(new QueryWrapper<AiChatMessageEntity>()
                    .eq("session_id", sessionId)
                    .orderByAsc("sequence_no"));

            List<Message> messages = new ArrayList<>();

            // 加载 System Prompt 并替换占位符
            SystemPromptTemplate promptTemplate = new SystemPromptTemplate(systemPromptResource);
            Message systemMessage = promptTemplate.createMessage(Map.of("patientId", session.getPatientId()));
            messages.add(systemMessage);

            for (AiChatMessageEntity msg : history) {
                if ("USER".equals(msg.getMessageRole())) {
                    messages.add(new UserMessage(msg.getMessageContent()));
                } else if ("ASSISTANT".equals(msg.getMessageRole())) {
                    messages.add(new AssistantMessage(msg.getMessageContent()));
                }
            }

            // 调用大模型
            String responseContent = chatClient.prompt()
                    .messages(messages)
                    .call()
                    .content();

            // 保存 AI 响应
            AiChatMessageEntity assistantMsgEntity = new AiChatMessageEntity();
            assistantMsgEntity.setSessionId(sessionId);
            assistantMsgEntity.setMessageRole("ASSISTANT");
            assistantMsgEntity.setMessageContent(responseContent);
            assistantMsgEntity.setSequenceNo(getSequenceNo(sessionId));
            assistantMsgEntity.setCreatedAt(LocalDateTime.now());
            messageService.save(assistantMsgEntity);

            return responseContent;
        } finally {
            AiAgentSessionContext.clear();
        }
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
}
