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

    /**
     * 获取患者的挂号记录
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.neusoft.neu23.neuhospital.registration.entity.RegistrationEntity> getMyRegistrations(Long patientId, int pageNo, int pageSize);

    /**
     * 获取医生或管理员视角的分页挂号记录
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.neusoft.neu23.neuhospital.registration.entity.RegistrationEntity> getAllRegistrations(int pageNo, int pageSize);

    /**
     * 获取挂号详情
     */
    com.neusoft.neu23.neuhospital.registration.entity.RegistrationEntity getRegistrationById(Long id);

    /**
     * 患者退号
     */
    void cancelRegistration(Long id, Long patientId);

    /**
     * 手动签到（就诊当日）
     */
    void checkIn(Long id, Long patientId);
}
