package com.neusoft.neu23.neuhospital.inspection.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.common.response.PageResult;
import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.inspection.dto.CheckRequestCreateReq;
import com.neusoft.neu23.neuhospital.inspection.service.CheckRequestService;
import com.neusoft.neu23.neuhospital.inspection.vo.CheckRequestVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/check-requests")
public class CheckRequestController {

    @Autowired
    private CheckRequestService checkRequestService;

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','MANAGEMENT')")
    public Result<Long> createRequest(@RequestBody CheckRequestCreateReq req) {
        Long id = checkRequestService.createRequest(req);
        return Result.success(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','MANAGEMENT')")
    public Result<CheckRequestVO> getRequestDetail(@PathVariable Long id) {
        return Result.success(checkRequestService.getRequestDetail(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','MANAGEMENT')")
    public Result<PageResult<CheckRequestVO>> getRequestsPage(
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam(value = "doctorId", required = false) Long doctorId) {
        Page<CheckRequestVO> page = checkRequestService.getRequestsPage(pageNo, pageSize, patientId, doctorId);
        return Result.success(new PageResult<>(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','MANAGEMENT')")
    public Result<Void> cancelRequest(@PathVariable Long id) {
        checkRequestService.cancelRequest(id);
        return Result.success(null);
    }
}
