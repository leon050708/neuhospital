package com.neusoft.neu23.neuhospital.ai.infrastructure.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neu23.neuhospital.ai.domain.entity.AiChatSessionEntity;
import com.neusoft.neu23.neuhospital.ai.infrastructure.mapper.AiChatSessionMapper;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.AiChatSessionService;
import org.springframework.stereotype.Service;

@Service
public class AiChatSessionServiceImpl extends ServiceImpl<AiChatSessionMapper, AiChatSessionEntity> implements AiChatSessionService {
}
