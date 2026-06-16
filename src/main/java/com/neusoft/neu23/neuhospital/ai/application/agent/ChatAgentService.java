package com.neusoft.neu23.neuhospital.ai.application.agent;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.neu23.neuhospital.ai.domain.entity.AiChatMessageEntity;
import com.neusoft.neu23.neuhospital.ai.domain.entity.AiChatSessionEntity;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.AiChatMessageService;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.AiChatSessionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatAgentService {

    private final ChatClient chatClient;
    private final AiChatSessionService sessionService;
    private final AiChatMessageService messageService;

    public ChatAgentService(ChatClient.Builder chatClientBuilder,
                            AiChatSessionService sessionService,
                            AiChatMessageService messageService) {
        this.chatClient = chatClientBuilder
                .defaultSystem("你是一个严谨的智慧医院问诊助手（Agent）。\n" +
                        "你可以向患者询问病情，或者解答初步的医疗疑惑。\n" +
                        "如果在聊天中发现患者提到了其个人明确的【过敏史】（如过敏原）或【慢性病史/既往史】（如高血压糖尿病），请主动调用工具(updatePatientMemory)将特征提取并永久记录到患者的结构化档案中。\n" +
                        "你必须通过患者ID调用工具了解患者基本盘。\n" +
                        "警告：你只能提供建议，不能代替医生做出最终确诊，且不能直接开具处方。")
                .defaultFunctions("getPatientInfo", "updatePatientMemory")
                .build();
        this.sessionService = sessionService;
        this.messageService = messageService;
    }

    public AiChatSessionEntity createSession(Long patientId, Long registrationId, String sessionType) {
        AiChatSessionEntity session = new AiChatSessionEntity();
        session.setSessionNo("CHAT" + System.currentTimeMillis());
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

    public String chat(Long sessionId, String userMessage) {
        AiChatSessionEntity session = sessionService.getById(sessionId);
        if (session == null) {
            throw new RuntimeException("Session not found");
        }

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
        for (AiChatMessageEntity msg : history) {
            if ("USER".equals(msg.getMessageRole())) {
                messages.add(new UserMessage(msg.getMessageContent()));
            } else if ("ASSISTANT".equals(msg.getMessageRole())) {
                messages.add(new AssistantMessage(msg.getMessageContent()));
            }
        }

        // 隐式注入当前病人的ID，方便大模型调用工具
        String contextPrompt = String.format("[系统级隐式上下文：当前与你对话的患者ID是 %d。如有需要，你可以调用工具查询或更新该患者的档案。]\n", session.getPatientId());
        
        // 替换最后一条消息，加上隐式上下文
        messages.remove(messages.size() - 1);
        messages.add(new UserMessage(contextPrompt + userMessage));

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
    }

    private Integer getSequenceNo(Long sessionId) {
        long count = messageService.count(new QueryWrapper<AiChatMessageEntity>().eq("session_id", sessionId));
        return (int) (count + 1);
    }
}
