package com.neusoft.neu23.neuhospital.registration.service;

import com.neusoft.neu23.neuhospital.registration.dto.RegistrationCreateReq;

public interface RegistrationService {
    /**
     * 极速挂号接口（Redis预扣减 -> 落地本地消息表 -> Kafka异步落库）
     */
    String quickRegister(RegistrationCreateReq req);
    
    /**
     * Kafka 消费者底层的落库业务
     */
    void processRegistrationMessage(String msgId, Long scheduleId, Long patientId);
}
