package com.neusoft.neu23.neuhospital.patient.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.common.response.PageResult;
import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.patient.dto.PatientCreateReq;
import com.neusoft.neu23.neuhospital.patient.dto.PatientUpdateReq;
import com.neusoft.neu23.neuhospital.patient.service.PatientService;
import com.neusoft.neu23.neuhospital.patient.vo.PatientVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
@Tag(name = "患者管理", description = "患者建档、患者信息更新与分页查询")
@SecurityRequirement(name = "bearerAuth")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    @Operation(summary = "新增患者", description = "创建患者档案并返回建档结果。")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGEMENT','REGISTRATION_CLERK')")
    public Result<PatientVO> createPatient(@RequestBody PatientCreateReq req) {
        PatientVO vo = patientService.createPatient(req);
        return Result.success(vo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询患者详情", description = "根据患者 ID 返回患者档案。")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGEMENT','REGISTRATION_CLERK','DOCTOR') or @accessEvaluator.isCurrentPatient(#id)")
    public Result<PatientVO> getPatient(@Parameter(description = "患者主键 ID", required = true) @PathVariable("id") Long id) {
        PatientVO vo = patientService.getPatientById(id);
        return Result.success(vo);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新患者信息", description = "根据患者 ID 修改患者资料。")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGEMENT','REGISTRATION_CLERK') or @accessEvaluator.isCurrentPatient(#id)")
    public Result<PatientVO> updatePatient(
            @Parameter(description = "患者主键 ID", required = true) @PathVariable("id") Long id,
            @RequestBody PatientUpdateReq req) {
        PatientVO vo = patientService.updatePatient(id, req);
        return Result.success(vo);
    }

    @GetMapping
    @Operation(summary = "分页查询患者", description = "按关键字分页检索患者档案。")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGEMENT','REGISTRATION_CLERK','DOCTOR')")
    public Result<PageResult<PatientVO>> getPatients(
            @Parameter(description = "页码，从 1 开始") @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @Parameter(description = "每页条数") @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @Parameter(description = "姓名、身份证号等关键字，可选") @RequestParam(value = "keyword", required = false) String keyword) {
        Page<PatientVO> page = patientService.getPatientsPage(pageNo, pageSize, keyword);
        return Result.success(new PageResult<>(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal()));
    }
}
