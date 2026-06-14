package com.neusoft.neu23.neuhospital.outpatient.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.common.response.PageResult;
import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.outpatient.dto.MedicalDiagnosisReq;
import com.neusoft.neu23.neuhospital.outpatient.dto.MedicalRecordCreateReq;
import com.neusoft.neu23.neuhospital.outpatient.dto.MedicalRecordUpdateReq;
import com.neusoft.neu23.neuhospital.outpatient.service.MedicalRecordService;
import com.neusoft.neu23.neuhospital.outpatient.vo.MedicalDiagnosisVO;
import com.neusoft.neu23.neuhospital.outpatient.vo.MedicalRecordVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/outpatient/records")
public class MedicalRecordController {

    @Autowired
    private MedicalRecordService medicalRecordService;

    @PostMapping
    public Result<Long> createRecord(@RequestBody MedicalRecordCreateReq req) {
        Long id = medicalRecordService.createRecord(req);
        return Result.success(id);
    }

    @GetMapping("/{id}")
    public Result<MedicalRecordVO> getRecordDetail(@PathVariable Long id) {
        MedicalRecordVO vo = medicalRecordService.getRecordDetail(id);
        return Result.success(vo);
    }

    @PutMapping("/{id}")
    public Result<Void> updateRecord(@PathVariable Long id, @RequestBody MedicalRecordUpdateReq req) {
        medicalRecordService.updateRecord(id, req);
        return Result.success(null);
    }

    @PostMapping("/{id}/confirm")
    public Result<Void> confirmRecord(@PathVariable Long id) {
        medicalRecordService.confirmRecord(id);
        return Result.success(null);
    }

    @GetMapping
    public Result<PageResult<MedicalRecordVO>> getRecordsPage(
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam(value = "doctorId", required = false) Long doctorId) {
        Page<MedicalRecordVO> page = medicalRecordService.getRecordsPage(pageNo, pageSize, patientId, doctorId);
        return Result.success(new PageResult<>(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal()));
    }

    @PostMapping("/{id}/diagnoses")
    public Result<Void> saveDiagnoses(@PathVariable Long id, @RequestBody List<MedicalDiagnosisReq> diagnoses) {
        medicalRecordService.saveDiagnoses(id, diagnoses);
        return Result.success(null);
    }

    @GetMapping("/{id}/diagnoses")
    public Result<List<MedicalDiagnosisVO>> getDiagnoses(@PathVariable Long id) {
        List<MedicalDiagnosisVO> diagnoses = medicalRecordService.getDiagnosesByRecordId(id);
        return Result.success(diagnoses);
    }
}
