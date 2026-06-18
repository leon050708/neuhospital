package com.neusoft.neu23.neuhospital.pharmacy.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.pharmacy.dto.PrescriptionCreateReq;
import com.neusoft.neu23.neuhospital.pharmacy.service.PrescriptionService;
import com.neusoft.neu23.neuhospital.pharmacy.vo.PrescriptionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    @Autowired
    private PrescriptionService prescriptionService;

    @PostMapping
    public Result<Long> createPrescription(@RequestBody PrescriptionCreateReq req) {
        return Result.success(prescriptionService.createPrescription(req));
    }

    @GetMapping("/{id}")
    public Result<PrescriptionVO> getPrescriptionDetail(@PathVariable Long id) {
        return Result.success(prescriptionService.getPrescriptionDetail(id));
    }

    @GetMapping
    public Result<Page<PrescriptionVO>> getPrescriptionsPage(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorId) {
        return Result.success(prescriptionService.getPrescriptionsPage(pageNo, pageSize, patientId, doctorId));
    }
}
