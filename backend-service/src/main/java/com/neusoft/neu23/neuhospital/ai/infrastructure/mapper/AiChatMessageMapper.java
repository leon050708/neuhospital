package com.neusoft.neu23.neuhospital.ai.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.neu23.neuhospital.ai.domain.entity.AiChatMessageEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessageEntity> {
}
