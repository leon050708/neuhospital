package com.neusoft.neu23.neuhospital.inspection.controller;

import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.inspection.dto.InspectionResultCreateReq;
import com.neusoft.neu23.neuhospital.inspection.service.InspectionResultService;
import com.neusoft.neu23.neuhospital.inspection.vo.InspectionResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inspection-results")
public class InspectionResultController {

    @Autowired
    private InspectionResultService inspectionResultService;

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','MANAGEMENT')")
    public Result<Long> recordResult(@RequestBody InspectionResultCreateReq req) {
        Long id = inspectionResultService.recordResult(req);
        return Result.success(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','MANAGEMENT')")
    public Result<InspectionResultVO> getResultDetail(@PathVariable Long id) {
        return Result.success(inspectionResultService.getResultDetail(id));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','MANAGEMENT')")
    public Result<Void> confirmResult(@PathVariable Long id) {
        inspectionResultService.confirmResult(id);
        return Result.success(null);
    }
}
