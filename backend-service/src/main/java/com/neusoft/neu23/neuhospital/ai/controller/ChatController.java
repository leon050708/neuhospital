package com.neusoft.neu23.neuhospital.ai.controller;

import com.neusoft.neu23.neuhospital.ai.application.agent.ChatAgentService;
import com.neusoft.neu23.neuhospital.ai.domain.entity.AiChatSessionEntity;
import com.neusoft.neu23.neuhospital.ai.dto.ChatSessionResp;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.AiChatSessionService;
import com.neusoft.neu23.neuhospital.auth.security.SecurityUtils;
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
    public Result<ChatSessionResp> createSession(@RequestBody ChatSessionCreateReq req) {
        Long currentPatientId;
        try {
            currentPatientId = SecurityUtils.getCurrentPatientId();
        } catch (IllegalStateException ex) {
            return Result.error(401, ex.getMessage());
        }
        try {
            AiChatSessionEntity session = chatAgentService.createSession(
                    currentPatientId,
                    req.getRegistrationId(),
                    req.getSessionType());
            return Result.success(ChatSessionResp.from(session));
        } catch (IllegalArgumentException ex) {
            return Result.error(404, ex.getMessage());
        }
    }

    @PostMapping("/{sessionNo}/messages")
    public Result<String> sendMessage(@PathVariable("sessionNo") String sessionNo,
                                      @RequestBody ChatMessageReq req) {
        Long currentPatientId;
        try {
            currentPatientId = SecurityUtils.getCurrentPatientId();
        } catch (IllegalStateException ex) {
            return Result.error(401, ex.getMessage());
        }
        try {
            String response = chatAgentService.chat(sessionNo, currentPatientId, req.getContent());
            return Result.success(response);
        } catch (IllegalArgumentException ex) {
            return Result.error(404, ex.getMessage());
        }
    }
}
