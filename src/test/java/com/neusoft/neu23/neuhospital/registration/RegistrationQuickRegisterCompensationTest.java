package com.neusoft.neu23.neuhospital.registration;

import com.neusoft.neu23.neuhospital.registration.dto.RegistrationCreateReq;
import com.neusoft.neu23.neuhospital.registration.entity.RegistrationMessageLogEntity;
import com.neusoft.neu23.neuhospital.registration.mapper.RegistrationMessageLogMapper;
import com.neusoft.neu23.neuhospital.registration.service.RegistrationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
public class RegistrationQuickRegisterCompensationTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockBean
    private RegistrationMessageLogMapper messageLogMapper;

    private String currentStockKey;
    private String currentLimitKey;

    @AfterEach
    void cleanupRedisKeys() {
        if (currentStockKey != null) {
            redisTemplate.delete(currentStockKey);
        }
        if (currentLimitKey != null) {
            redisTemplate.delete(currentLimitKey);
        }
    }

    @Test
    void quickRegister_shouldCompensateRedisStockWhenMessageLogInsertFails() {
        Long scheduleId = System.currentTimeMillis();
        Long patientId = scheduleId + 1;
        currentStockKey = "schedule:stock:" + scheduleId;
        currentLimitKey = "rate_limit:registration:" + scheduleId;

        redisTemplate.opsForValue().set(currentStockKey, "1");
        redisTemplate.delete(currentLimitKey);
        when(messageLogMapper.insertWithJsonPayload(any(RegistrationMessageLogEntity.class)))
                .thenThrow(new RuntimeException("mock insert failure"));

        RegistrationCreateReq req = new RegistrationCreateReq();
        req.setScheduleId(scheduleId);
        req.setPatientId(patientId);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> registrationService.quickRegister(req));

        assertEquals("mock insert failure", exception.getMessage());
        assertEquals("1", redisTemplate.opsForValue().get(currentStockKey), "事务回滚后 Redis 库存必须补回");
        verify(messageLogMapper).insertWithJsonPayload(any(RegistrationMessageLogEntity.class));
    }
}
