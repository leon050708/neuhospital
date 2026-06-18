package com.neusoft.neu23.neuhospital.registration;

import com.neusoft.neu23.neuhospital.registration.dto.RegistrationCreateReq;
import com.neusoft.neu23.neuhospital.registration.entity.RegistrationMessageLogEntity;
import com.neusoft.neu23.neuhospital.registration.mapper.RegistrationMessageLogMapper;
import com.neusoft.neu23.neuhospital.registration.service.RegistrationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class RegistrationQuickRegisterPersistenceTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private RegistrationMessageLogMapper messageLogMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String currentMsgId;
    private String currentStockKey;
    private String currentLimitKey;

    @AfterEach
    void cleanup() {
        if (currentMsgId != null) {
            messageLogMapper.deleteById(currentMsgId);
        }
        if (currentStockKey != null) {
            redisTemplate.delete(currentStockKey);
        }
        if (currentLimitKey != null) {
            redisTemplate.delete(currentLimitKey);
        }
    }

    @Test
    void quickRegister_shouldPersistMessageLogIntoJsonPayloadColumn() {
        Long scheduleId = System.currentTimeMillis();
        Long patientId = scheduleId + 10;
        currentStockKey = "schedule:stock:" + scheduleId;
        currentLimitKey = "rate_limit:registration:" + scheduleId;

        redisTemplate.opsForValue().set(currentStockKey, "1");
        redisTemplate.delete(currentLimitKey);

        RegistrationCreateReq req = new RegistrationCreateReq();
        req.setScheduleId(scheduleId);
        req.setPatientId(patientId);

        currentMsgId = registrationService.quickRegister(req);

        RegistrationMessageLogEntity saved = messageLogMapper.selectById(currentMsgId);
        assertNotNull(saved, "quickRegister 成功后应真实写入本地消息表");
        assertEquals(scheduleId, saved.getScheduleId());
        assertEquals(patientId, saved.getPatientId());
        assertNotNull(saved.getPayload(), "payload 应落库成功");
        assertTrue(saved.getPayload().contains("\"scheduleId\":" + scheduleId), "payload 中应包含排班号");
        assertTrue(saved.getPayload().contains("\"patientId\":" + patientId), "payload 中应包含患者号");
        assertEquals("0", redisTemplate.opsForValue().get(currentStockKey), "消息落库成功后 Redis 库存应被预扣");
    }
}
