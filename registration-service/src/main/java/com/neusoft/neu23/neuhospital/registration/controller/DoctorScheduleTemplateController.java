package com.neusoft.neu23.neuhospital.registration.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.common.response.PageResult;
import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.registration.dto.ScheduleTemplateCreateReq;
import com.neusoft.neu23.neuhospital.registration.dto.ScheduleTemplateUpdateReq;
import com.neusoft.neu23.neuhospital.registration.service.DoctorScheduleTemplateService;
import com.neusoft.neu23.neuhospital.registration.vo.ScheduleTemplateVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schedule-templates")
public class DoctorScheduleTemplateController {

    private final DoctorScheduleTemplateService templateService;

    public DoctorScheduleTemplateController(DoctorScheduleTemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGEMENT','REGISTRATION_CLERK')")
    public Result<ScheduleTemplateVO> createTemplate(@RequestBody ScheduleTemplateCreateReq req) {
        return Result.success(templateService.createTemplate(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGEMENT','REGISTRATION_CLERK')")
    public Result<ScheduleTemplateVO> getTemplate(@PathVariable Long id) {
        return Result.success(templateService.getTemplate(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGEMENT','REGISTRATION_CLERK')")
    public Result<PageResult<ScheduleTemplateVO>> listTemplates(
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "doctorId", required = false) Long doctorId,
            @RequestParam(value = "departmentId", required = false) Long departmentId) {
        Page<ScheduleTemplateVO> page = templateService.listTemplates(pageNo, pageSize, doctorId, departmentId);
        return Result.success(new PageResult<>(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGEMENT','REGISTRATION_CLERK')")
    public Result<ScheduleTemplateVO> updateTemplate(@PathVariable Long id, @RequestBody ScheduleTemplateUpdateReq req) {
        return Result.success(templateService.updateTemplate(id, req));
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGEMENT','REGISTRATION_CLERK')")
    public Result<Void> disableTemplate(@PathVariable Long id) {
        templateService.disableTemplate(id);
        return Result.success(null);
    }
}
