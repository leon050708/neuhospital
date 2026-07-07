package com.neusoft.neu23.neuhospital.registration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.doctor.entity.DepartmentEntity;
import com.neusoft.neu23.neuhospital.doctor.entity.DoctorEntity;
import com.neusoft.neu23.neuhospital.doctor.mapper.DepartmentMapper;
import com.neusoft.neu23.neuhospital.doctor.mapper.DoctorMapper;
import com.neusoft.neu23.neuhospital.registration.entity.DoctorScheduleEntity;
import com.neusoft.neu23.neuhospital.registration.mapper.DoctorScheduleMapper;
import com.neusoft.neu23.neuhospital.registration.vo.DoctorScheduleVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorScheduleServiceImplTest {

    @Test
    void shouldOnlyReturnSchedulablePatientSchedules() {
        DoctorScheduleMapper doctorScheduleMapper = mock(DoctorScheduleMapper.class);
        DoctorMapper doctorMapper = mock(DoctorMapper.class);
        DepartmentMapper departmentMapper = mock(DepartmentMapper.class);
        DoctorScheduleServiceImpl service = new DoctorScheduleServiceImpl(
                doctorScheduleMapper,
                doctorMapper,
                departmentMapper
        );

        LocalDate today = LocalDate.now();
        DoctorScheduleEntity expired = schedule(1L, today.minusDays(1), 3, "ENABLED");
        DoctorScheduleEntity noQuota = schedule(2L, today.plusDays(1), 0, "ENABLED");
        DoctorScheduleEntity closed = schedule(3L, today.plusDays(1), 2, "CLOSED");
        DoctorScheduleEntity available = schedule(4L, today.plusDays(2), 5, "ENABLED");

        doAnswer(invocation -> {
            Page<DoctorScheduleEntity> page = invocation.getArgument(0);
            page.setRecords(List.of(expired, noQuota, closed, available));
            page.setTotal(4);
            return page;
        }).when(doctorScheduleMapper).selectPage(any(Page.class), any(QueryWrapper.class));

        DoctorEntity doctor = new DoctorEntity();
        doctor.setId(11L);
        doctor.setName("张医生");
        when(doctorMapper.selectBatchIds(any())).thenReturn(List.of(doctor));

        DepartmentEntity department = new DepartmentEntity();
        department.setId(21L);
        department.setDeptName("消化内科");
        when(departmentMapper.selectBatchIds(any())).thenReturn(List.of(department));

        Page<DoctorScheduleVO> result = service.getSchedulesPage(1, 10, null, 21L, null, null);

        assertEquals(1, result.getRecords().size());
        assertEquals(4L, result.getRecords().get(0).getId());

        ArgumentCaptor<QueryWrapper<DoctorScheduleEntity>> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(doctorScheduleMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("schedule_date"));
        assertTrue(sqlSegment.contains("available_count"));
        assertTrue(sqlSegment.contains("status"));
    }

    private DoctorScheduleEntity schedule(Long id, LocalDate scheduleDate, Integer availableCount, String status) {
        DoctorScheduleEntity entity = new DoctorScheduleEntity();
        entity.setId(id);
        entity.setDoctorId(11L);
        entity.setDepartmentId(21L);
        entity.setScheduleDate(scheduleDate);
        entity.setTimeSlot("上午");
        entity.setAvailableCount(availableCount);
        entity.setSourceCount(10);
        entity.setStatus(status);
        return entity;
    }
}
