package com.neusoft.neu23.neuhospital.patient.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.common.response.PageResult;
import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.patient.dto.PatientCreateReq;
import com.neusoft.neu23.neuhospital.patient.dto.PatientUpdateReq;
import com.neusoft.neu23.neuhospital.patient.service.PatientService;
import com.neusoft.neu23.neuhospital.patient.vo.PatientVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    public Result<PatientVO> createPatient(@RequestBody PatientCreateReq req) {
        PatientVO vo = patientService.createPatient(req);
        return Result.success(vo);
    }

    @GetMapping("/{id}")
    public Result<PatientVO> getPatient(@PathVariable("id") Long id) {
        PatientVO vo = patientService.getPatientById(id);
        return Result.success(vo);
    }

    @PutMapping("/{id}")
    public Result<PatientVO> updatePatient(@PathVariable("id") Long id, @RequestBody PatientUpdateReq req) {
        PatientVO vo = patientService.updatePatient(id, req);
        return Result.success(vo);
    }

    @GetMapping
    public Result<PageResult<PatientVO>> getPatients(
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        Page<PatientVO> page = patientService.getPatientsPage(pageNo, pageSize, keyword);
        return Result.success(new PageResult<>(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal()));
    }
}
