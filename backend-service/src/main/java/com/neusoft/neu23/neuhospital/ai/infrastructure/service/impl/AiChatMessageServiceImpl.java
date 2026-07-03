package com.neusoft.neu23.neuhospital.ai.infrastructure.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neu23.neuhospital.ai.domain.entity.AiChatMessageEntity;
import com.neusoft.neu23.neuhospital.ai.infrastructure.mapper.AiChatMessageMapper;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.AiChatMessageService;
import org.springframework.stereotype.Service;

@Service
public class AiChatMessageServiceImpl extends ServiceImpl<AiChatMessageMapper, AiChatMessageEntity> implements AiChatMessageService {
}
