package com.neusoft.neu23.neuhospital.inspection.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.common.response.PageResult;
import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.inspection.dto.InspectionRequestCreateReq;
import com.neusoft.neu23.neuhospital.inspection.service.InspectionRequestService;
import com.neusoft.neu23.neuhospital.inspection.vo.InspectionRequestVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inspection-requests")
public class InspectionRequestController {

    @Autowired
    private InspectionRequestService inspectionRequestService;

    @PostMapping
    public Result<Long> createRequest(@RequestBody InspectionRequestCreateReq req) {
        Long id = inspectionRequestService.createRequest(req);
        return Result.success(id);
    }

    @GetMapping("/{id}")
    public Result<InspectionRequestVO> getRequestDetail(@PathVariable Long id) {
        return Result.success(inspectionRequestService.getRequestDetail(id));
    }

    @GetMapping
    public Result<PageResult<InspectionRequestVO>> getRequestsPage(
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam(value = "doctorId", required = false) Long doctorId) {
        Page<InspectionRequestVO> page = inspectionRequestService.getRequestsPage(pageNo, pageSize, patientId, doctorId);
        return Result.success(new PageResult<>(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal()));
    }
}
