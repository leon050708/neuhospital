package com.neusoft.neu23.neuhospital.registration.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.neu23.neuhospital.registration.dto.RegistrationMessagePayload;
import com.neusoft.neu23.neuhospital.registration.service.RegistrationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class RegistrationMessageConsumer {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = RegistrationMessageProducer.TOPIC, groupId = "registration_group")
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            String jsonValue = record.value();
            RegistrationMessagePayload payload = objectMapper.readValue(jsonValue, RegistrationMessagePayload.class);

            // 执行核心落盘 DB 事务
            registrationService.processRegistrationMessage(payload.getMsgId(), payload.getScheduleId(), payload.getPatientId());
            
            // 成功后必须手动 ACK (前提：配置文件中 enable.auto.commit 必须为 false，且 ack-mode 为 MANUAL)
            ack.acknowledge();
        } catch (Exception e) {
            // 这里如果发生异常，且不手动 ACK，Kafka 会在稍后重投。
            // 实际生产中应配合死信队列 (DLQ) 或 Spring Retry 进行逆向补偿，回滚 Redis 库存。
            System.err.println("消息消费失败，进入重试机制: " + e.getMessage());
            // 注意这里不抛出异常会导致offset没变化卡住，或者抛异常触发 spring retry
            throw new RuntimeException("触发重试", e);
        }
    }
}
