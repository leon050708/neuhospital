package com.neusoft.neu23.neuhospital.ai.controller;

import com.neusoft.neu23.neuhospital.ai.application.agent.ChatAgentService;
import com.neusoft.neu23.neuhospital.ai.domain.entity.AiChatSessionEntity;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.AiChatSessionService;
import com.neusoft.neu23.neuhospital.ai.dto.ChatMessageReq;
import com.neusoft.neu23.neuhospital.ai.dto.ChatSessionCreateReq;
import com.neusoft.neu23.neuhospital.common.response.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/chat/sessions")
public class ChatController {

    private final ChatAgentService chatAgentService;
    private final AiChatSessionService sessionService;

    public ChatController(ChatAgentService chatAgentService,
                          AiChatSessionService sessionService) {
        this.chatAgentService = chatAgentService;
        this.sessionService = sessionService;
    }

    @PostMapping
    public Result<AiChatSessionEntity> createSession(@RequestBody ChatSessionCreateReq req) {
        Long currentPatientId = com.neusoft.neu23.neuhospital.auth.security.SecurityUtils.getCurrentUserId();
        if (currentPatientId == null) {
            return Result.error(401, "未登录或会话已过期");
        }
        AiChatSessionEntity session = chatAgentService.createSession(
                currentPatientId,
                req.getRegistrationId(),
                req.getSessionType());
        return Result.success(session);
    }

    @PostMapping("/{id}/messages")
    public Result<String> sendMessage(@PathVariable("id") Long sessionId,
                                      @RequestBody ChatMessageReq req) {
        AiChatSessionEntity session = sessionService.getById(sessionId);
        if (session == null) {
            return Result.error(404, "会话不存在");
        }

        String response = chatAgentService.chat(sessionId, req.getContent());
        
        return Result.success(response);
    }
}
