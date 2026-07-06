package com.neusoft.neu23.neuhospital.ai.infrastructure.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neu23.neuhospital.ai.domain.entity.KnowledgeChunkEntity;
import com.neusoft.neu23.neuhospital.ai.infrastructure.mapper.KnowledgeChunkMapper;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.KnowledgeChunkService;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeChunkServiceImpl extends ServiceImpl<KnowledgeChunkMapper, KnowledgeChunkEntity>
        implements KnowledgeChunkService {
}
