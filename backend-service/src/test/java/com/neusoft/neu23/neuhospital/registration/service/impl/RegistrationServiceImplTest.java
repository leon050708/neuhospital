package com.neusoft.neu23.neuhospital.registration.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisMapperBuilderAssistant;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.neusoft.neu23.neuhospital.registration.entity.DoctorScheduleEntity;
import com.neusoft.neu23.neuhospital.registration.entity.RegistrationEntity;
import com.neusoft.neu23.neuhospital.registration.entity.RegistrationMessageLogEntity;
import com.neusoft.neu23.neuhospital.registration.mapper.DoctorScheduleMapper;
import com.neusoft.neu23.neuhospital.registration.mapper.RegistrationMapper;
import com.neusoft.neu23.neuhospital.registration.mapper.RegistrationMessageLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RegistrationMessageLogMapper messageLogMapper;

    @Mock
    private DoctorScheduleMapper doctorScheduleMapper;

    @Mock
    private RegistrationMapper registrationMapper;

    @BeforeEach
    void setUp() {
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        MybatisMapperBuilderAssistant assistant = new MybatisMapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, RegistrationMessageLogEntity.class);
        TableInfoHelper.initTableInfo(assistant, RegistrationEntity.class);
    }

    @Test
    void shouldRejectExpiredScheduleWhenProcessingRegistrationMessage() {
        RegistrationMessageLogEntity messageLog = new RegistrationMessageLogEntity();
        messageLog.setMsgId("MSG-001");
        messageLog.setStatus(0);
        messageLog.setRetryCount(0);
        when(messageLogMapper.selectById("MSG-001")).thenReturn(messageLog);
        when(registrationMapper.selectCount(any())).thenReturn(0L);

        DoctorScheduleEntity expiredSchedule = new DoctorScheduleEntity();
        expiredSchedule.setId(66L);
        expiredSchedule.setDoctorId(101L);
        expiredSchedule.setDepartmentId(201L);
        expiredSchedule.setScheduleDate(LocalDate.now().minusDays(1));
        expiredSchedule.setAvailableCount(5);
        expiredSchedule.setStatus("ENABLED");
        when(doctorScheduleMapper.selectById(66L)).thenReturn(expiredSchedule);

        registrationService.processRegistrationMessage("MSG-001", 66L, 88L);

        verify(messageLogMapper).update(any(), any());
        verify(doctorScheduleMapper, never()).updateById(any());
        verify(registrationMapper, never()).insert(any());
    }

    @Test
    void shouldRejectClosedScheduleWhenProcessingRegistrationMessage() {
        RegistrationMessageLogEntity messageLog = new RegistrationMessageLogEntity();
        messageLog.setMsgId("MSG-002");
        messageLog.setStatus(0);
        messageLog.setRetryCount(0);
        when(messageLogMapper.selectById("MSG-002")).thenReturn(messageLog);
        when(registrationMapper.selectCount(any())).thenReturn(0L);

        DoctorScheduleEntity closedSchedule = new DoctorScheduleEntity();
        closedSchedule.setId(67L);
        closedSchedule.setDoctorId(101L);
        closedSchedule.setDepartmentId(201L);
        closedSchedule.setScheduleDate(LocalDate.now().plusDays(1));
        closedSchedule.setAvailableCount(5);
        closedSchedule.setStatus("CLOSED");
        when(doctorScheduleMapper.selectById(67L)).thenReturn(closedSchedule);

        registrationService.processRegistrationMessage("MSG-002", 67L, 89L);

        verify(messageLogMapper).update(any(), any());
        verify(doctorScheduleMapper, never()).updateById(any());
        verify(registrationMapper, never()).insert(any());
    }
}
