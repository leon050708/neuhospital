package com.neusoft.neu23.neuhospital.registration.mq;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.neu23.neuhospital.registration.dto.RegistrationMessagePayload;
import com.neusoft.neu23.neuhospital.registration.entity.RegistrationMessageLogEntity;
import com.neusoft.neu23.neuhospital.registration.mapper.RegistrationMessageLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@EnableScheduling
public class RegistrationMessageProducer {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private RegistrationMessageLogMapper messageLogMapper;

    @Autowired
    private ObjectMapper objectMapper;

    public static final String TOPIC = "topic_registration_create";

    // 每隔 2 秒钟扫描一次本地消息表中待发送的消息
    @Scheduled(fixedDelay = 2000)
    public void scanAndSendMessages() {
        QueryWrapper<RegistrationMessageLogEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 0) // 0-待发送
               .lt("retry_count", 3)
               .last("LIMIT 100");
        
        List<RegistrationMessageLogEntity> list = messageLogMapper.selectList(wrapper);
        if (list == null || list.isEmpty()) return;

        for (RegistrationMessageLogEntity log : list) {
            try {
                RegistrationMessagePayload payload = new RegistrationMessagePayload();
                payload.setMsgId(log.getMsgId());
                payload.setScheduleId(log.getScheduleId());
                payload.setPatientId(log.getPatientId());

                String jsonMsg = objectMapper.writeValueAsString(payload);
                // 投递 Kafka
                kafkaTemplate.send(TOPIC, log.getMsgId(), jsonMsg).whenComplete((res, ex) -> {
                    if (ex == null) {
                        // 发送成功，更新本地表
                        log.setStatus(1); // 已发送
                        messageLogMapper.updateById(log);
                    } else {
                        // 发送失败，增加重试次数
                        log.setRetryCount(log.getRetryCount() + 1);
                        messageLogMapper.updateById(log);
                    }
                });
            } catch (JsonProcessingException e) {
                log.setRetryCount(log.getRetryCount() + 1);
                messageLogMapper.updateById(log);
            }
        }
    }
}
