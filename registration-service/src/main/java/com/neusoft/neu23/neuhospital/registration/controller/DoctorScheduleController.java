package com.neusoft.neu23.neuhospital.registration.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.common.response.PageResult;
import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.registration.dto.DoctorScheduleCreateReq;
import com.neusoft.neu23.neuhospital.registration.dto.DoctorScheduleUpdateReq;
import com.neusoft.neu23.neuhospital.registration.service.DoctorScheduleService;
import com.neusoft.neu23.neuhospital.registration.vo.DoctorScheduleVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/schedules")
public class DoctorScheduleController {

    private final DoctorScheduleService doctorScheduleService;

    public DoctorScheduleController(DoctorScheduleService doctorScheduleService) {
        this.doctorScheduleService = doctorScheduleService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGEMENT','REGISTRATION_CLERK')")
    public Result<DoctorScheduleVO> createSchedule(@RequestBody DoctorScheduleCreateReq req) {
        DoctorScheduleVO vo = doctorScheduleService.createSchedule(req);
        return Result.success(vo);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGEMENT','REGISTRATION_CLERK')")
    public Result<DoctorScheduleVO> updateSchedule(@PathVariable("id") Long id, @RequestBody DoctorScheduleUpdateReq req) {
        DoctorScheduleVO vo = doctorScheduleService.updateSchedule(id, req);
        return Result.success(vo);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGEMENT','REGISTRATION_CLERK')")
    public Result<Void> closeSchedule(@PathVariable("id") Long id) {
        doctorScheduleService.closeSchedule(id);
        return Result.success(null);
    }

    @GetMapping
    public Result<PageResult<DoctorScheduleVO>> getSchedulesPage(
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "doctorId", required = false) Long doctorId,
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "scheduleDate", required = false) LocalDate scheduleDate,
            @RequestParam(value = "timeSlot", required = false) String timeSlot) {
        Page<DoctorScheduleVO> page = doctorScheduleService.getSchedulesPage(pageNo, pageSize, doctorId, departmentId, scheduleDate, timeSlot);
        return Result.success(new PageResult<>(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal()));
    }
}
