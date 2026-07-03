package com.neusoft.neu23.neuhospital.inspection.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.common.response.PageResult;
import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.inspection.dto.DisposalRequestCreateReq;
import com.neusoft.neu23.neuhospital.inspection.service.DisposalRequestService;
import com.neusoft.neu23.neuhospital.inspection.vo.DisposalRequestVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/disposal-requests")
public class DisposalRequestController {

    @Autowired
    private DisposalRequestService disposalRequestService;

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','MANAGEMENT')")
    public Result<Long> createRequest(@RequestBody DisposalRequestCreateReq req) {
        Long id = disposalRequestService.createRequest(req);
        return Result.success(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','MANAGEMENT')")
    public Result<DisposalRequestVO> getRequestDetail(@PathVariable Long id) {
        return Result.success(disposalRequestService.getRequestDetail(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','MANAGEMENT')")
    public Result<PageResult<DisposalRequestVO>> getRequestsPage(
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam(value = "doctorId", required = false) Long doctorId) {
        Page<DisposalRequestVO> page = disposalRequestService.getRequestsPage(pageNo, pageSize, patientId, doctorId);
        return Result.success(new PageResult<>(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','MANAGEMENT')")
    public Result<Void> cancelRequest(@PathVariable Long id) {
        disposalRequestService.cancelRequest(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/finish")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','MANAGEMENT')")
    public Result<Void> finishRequest(@PathVariable Long id) {
        disposalRequestService.finishRequest(id);
        return Result.success(null);
    }
}
