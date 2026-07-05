package com.neusoft.neu23.neuhospital.registration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.neu23.neuhospital.registration.config.RegistrationScheduleProperties;
import com.neusoft.neu23.neuhospital.registration.dto.DoctorScheduleCreateReq;
import com.neusoft.neu23.neuhospital.registration.dto.ScheduleBatchGenerateReq;
import com.neusoft.neu23.neuhospital.registration.entity.DoctorScheduleEntity;
import com.neusoft.neu23.neuhospital.registration.entity.DoctorScheduleTemplateEntity;
import com.neusoft.neu23.neuhospital.registration.mapper.DoctorScheduleMapper;
import com.neusoft.neu23.neuhospital.registration.mapper.DoctorScheduleTemplateMapper;
import com.neusoft.neu23.neuhospital.registration.service.DoctorScheduleService;
import com.neusoft.neu23.neuhospital.registration.service.ScheduleGenerateService;
import com.neusoft.neu23.neuhospital.registration.vo.DoctorScheduleVO;
import com.neusoft.neu23.neuhospital.registration.vo.ScheduleGenerateResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleGenerateServiceImplTest {

    @Mock
    private DoctorScheduleTemplateMapper templateMapper;
    @Mock
    private DoctorScheduleMapper doctorScheduleMapper;
    @Mock
    private DoctorScheduleService doctorScheduleService;

    private RegistrationScheduleProperties scheduleProperties;
    private ScheduleGenerateService scheduleGenerateService;

    @BeforeEach
    void setUp() {
        scheduleProperties = new RegistrationScheduleProperties();
        scheduleProperties.setAdvanceDays(7);
        scheduleGenerateService = new ScheduleGenerateServiceImpl(
                templateMapper, doctorScheduleMapper, doctorScheduleService, scheduleProperties);
    }

    @Test
    void generateFromTemplates_shouldCreateMatchingDaysAndSkipDuplicates() {
        LocalDate monday = LocalDate.of(2026, 7, 6);
        DoctorScheduleTemplateEntity template = new DoctorScheduleTemplateEntity();
        template.setDoctorId(9201L);
        template.setDepartmentId(9101L);
        template.setDayOfWeek(1);
        template.setTimeSlot("MORNING");
        template.setSourceCount(20);
        template.setFeeAmount(new BigDecimal("30.00"));
        template.setSourceType("NORMAL");
        when(templateMapper.selectList(any())).thenReturn(List.of(template));
        when(doctorScheduleMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(doctorScheduleService.createSchedule(any(DoctorScheduleCreateReq.class))).thenAnswer(invocation -> {
            DoctorScheduleCreateReq req = invocation.getArgument(0);
            DoctorScheduleVO vo = new DoctorScheduleVO();
            vo.setId(9900L + req.getScheduleDate().getDayOfMonth());
            vo.setScheduleDate(req.getScheduleDate());
            return vo;
        });

        ScheduleBatchGenerateReq req = new ScheduleBatchGenerateReq();
        req.setStartDate(monday);
        req.setDays(7);

        ScheduleGenerateResultVO result = scheduleGenerateService.generateFromTemplates(req);

        assertEquals(monday, result.getStartDate());
        assertEquals(monday.plusDays(6), result.getEndDate());
        assertEquals(1, result.getCreatedCount());
        assertEquals(0, result.getSkippedCount());

        ArgumentCaptor<DoctorScheduleCreateReq> captor = ArgumentCaptor.forClass(DoctorScheduleCreateReq.class);
        verify(doctorScheduleService, times(1)).createSchedule(captor.capture());
        assertEquals(monday, captor.getValue().getScheduleDate());
        assertEquals(9201L, captor.getValue().getDoctorId());
    }

    @Test
    void generateFromTemplates_shouldSkipWhenScheduleAlreadyExists() {
        LocalDate monday = LocalDate.of(2026, 7, 6);
        DoctorScheduleTemplateEntity template = new DoctorScheduleTemplateEntity();
        template.setDoctorId(9201L);
        template.setDepartmentId(9101L);
        template.setDayOfWeek(1);
        template.setTimeSlot("MORNING");
        template.setSourceCount(20);
        template.setFeeAmount(new BigDecimal("30.00"));
        when(templateMapper.selectList(any())).thenReturn(List.of(template));
        when(doctorScheduleMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        ScheduleBatchGenerateReq req = new ScheduleBatchGenerateReq();
        req.setStartDate(monday);
        req.setDays(7);

        ScheduleGenerateResultVO result = scheduleGenerateService.generateFromTemplates(req);

        assertEquals(0, result.getCreatedCount());
        assertEquals(1, result.getSkippedCount());
        verify(doctorScheduleService, never()).createSchedule(any());
    }
}
