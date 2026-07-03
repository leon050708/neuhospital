package com.neusoft.neu23.neuhospital.registration.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.registration.dto.DoctorScheduleCreateReq;
import com.neusoft.neu23.neuhospital.registration.dto.DoctorScheduleUpdateReq;
import com.neusoft.neu23.neuhospital.registration.vo.DoctorScheduleVO;

import java.time.LocalDate;

public interface DoctorScheduleService {
    DoctorScheduleVO createSchedule(DoctorScheduleCreateReq req);
    DoctorScheduleVO updateSchedule(Long id, DoctorScheduleUpdateReq req);
    void closeSchedule(Long id);
    Page<DoctorScheduleVO> getSchedulesPage(Integer pageNo, Integer pageSize, Long doctorId, Long departmentId, LocalDate scheduleDate, String timeSlot);
}
