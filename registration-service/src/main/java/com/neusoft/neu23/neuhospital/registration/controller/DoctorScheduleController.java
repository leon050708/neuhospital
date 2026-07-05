package com.neusoft.neu23.neuhospital.registration.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.common.response.PageResult;
import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.registration.dto.DoctorScheduleCreateReq;
import com.neusoft.neu23.neuhospital.registration.dto.DoctorScheduleUpdateReq;
import com.neusoft.neu23.neuhospital.registration.dto.ScheduleBatchGenerateReq;
import com.neusoft.neu23.neuhospital.registration.service.DoctorScheduleService;
import com.neusoft.neu23.neuhospital.registration.service.ScheduleGenerateService;
import com.neusoft.neu23.neuhospital.registration.vo.DoctorScheduleVO;
import com.neusoft.neu23.neuhospital.registration.vo.ScheduleGenerateResultVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/schedules")
public class DoctorScheduleController {

    private final DoctorScheduleService doctorScheduleService;
    private final ScheduleGenerateService scheduleGenerateService;

    public DoctorScheduleController(DoctorScheduleService doctorScheduleService,
                                    ScheduleGenerateService scheduleGenerateService) {
        this.doctorScheduleService = doctorScheduleService;
        this.scheduleGenerateService = scheduleGenerateService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGEMENT','REGISTRATION_CLERK')")
    public Result<DoctorScheduleVO> createSchedule(@RequestBody DoctorScheduleCreateReq req) {
        DoctorScheduleVO vo = doctorScheduleService.createSchedule(req);
        return Result.success(vo);
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGEMENT','REGISTRATION_CLERK')")
    public Result<ScheduleGenerateResultVO> generateSchedules(@RequestBody(required = false) ScheduleBatchGenerateReq req) {
        ScheduleBatchGenerateReq request = req != null ? req : new ScheduleBatchGenerateReq();
        return Result.success(scheduleGenerateService.generateFromTemplates(request));
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
            @RequestParam(value = "timeSlot", required = false) String timeSlot,
            @RequestParam(value = "bookableOnly", defaultValue = "false") Boolean bookableOnly) {
        Page<DoctorScheduleVO> page = doctorScheduleService.getSchedulesPage(
                pageNo, pageSize, doctorId, departmentId, scheduleDate, timeSlot, bookableOnly);
        return Result.success(new PageResult<>(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal()));
    }
}
