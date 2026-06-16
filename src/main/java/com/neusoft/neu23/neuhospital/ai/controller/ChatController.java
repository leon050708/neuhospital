package com.neusoft.neu23.neuhospital.ai.controller;

import com.neusoft.neu23.neuhospital.ai.application.agent.ChatAgentService;
import com.neusoft.neu23.neuhospital.ai.domain.entity.AiChatSessionEntity;
import com.neusoft.neu23.neuhospital.ai.dto.ChatMessageReq;
import com.neusoft.neu23.neuhospital.ai.dto.ChatSessionCreateReq;
import com.neusoft.neu23.neuhospital.common.response.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/chat/sessions")
public class ChatController {

    private final ChatAgentService chatAgentService;

    public ChatController(ChatAgentService chatAgentService) {
        this.chatAgentService = chatAgentService;
    }

    @PostMapping
    public Result<AiChatSessionEntity> createSession(@RequestBody ChatSessionCreateReq req) {
        AiChatSessionEntity session = chatAgentService.createSession(
                req.getPatientId(),
                req.getRegistrationId(),
                req.getSessionType());
        return Result.success(session);
    }

    @PostMapping("/{id}/messages")
    public Result<String> sendMessage(@PathVariable("id") Long sessionId,
                                      @RequestBody ChatMessageReq req) {
        String response = chatAgentService.chat(sessionId, req.getContent());
        return Result.success(response);
    }
}
