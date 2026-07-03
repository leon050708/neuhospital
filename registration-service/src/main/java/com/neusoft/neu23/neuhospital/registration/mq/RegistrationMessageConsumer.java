package com.neusoft.neu23.neuhospital.registration.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.neu23.neuhospital.registration.dto.RegistrationMessagePayload;
import com.neusoft.neu23.neuhospital.registration.service.RegistrationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RegistrationMessageConsumer {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = RegistrationMessageProducer.TOPIC, groupId = "registration_group")
    public void onMessage(ConsumerRecord<String, String> record) {
        try {
            String jsonValue = record.value();
            RegistrationMessagePayload payload = objectMapper.readValue(jsonValue, RegistrationMessagePayload.class);

            // 执行核心落盘 DB 事务
            registrationService.processRegistrationMessage(payload.getMsgId(), payload.getScheduleId(), payload.getPatientId());
        } catch (Exception e) {
            System.err.println("消息消费失败，进入重试机制: " + e.getMessage());
            throw new RuntimeException("触发重试", e);
        }
    }
}
