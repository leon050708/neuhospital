package com.neusoft.neu23.neuhospital.doctor.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.common.response.PageResult;
import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.doctor.dto.DoctorCreateReq;
import com.neusoft.neu23.neuhospital.doctor.dto.DoctorUpdateReq;
import com.neusoft.neu23.neuhospital.doctor.service.DoctorService;
import com.neusoft.neu23.neuhospital.doctor.vo.DoctorVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@Tag(name = "医生管理", description = "医生档案维护、分页查询与按科室筛选")
@SecurityRequirement(name = "bearerAuth")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @PostMapping
    @Operation(summary = "新增医生", description = "创建一条新的医生档案记录。")
    public Result<DoctorVO> createDoctor(@RequestBody DoctorCreateReq req) {
        DoctorVO vo = doctorService.createDoctor(req);
        return Result.success(vo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询医生详情", description = "根据医生 ID 返回医生的完整档案信息。")
    public Result<DoctorVO> getDoctor(@Parameter(description = "医生主键 ID", required = true) @PathVariable("id") Long id) {
        DoctorVO vo = doctorService.getDoctorById(id);
        return Result.success(vo);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新医生信息", description = "根据医生 ID 修改医生资料。")
    public Result<DoctorVO> updateDoctor(
            @Parameter(description = "医生主键 ID", required = true) @PathVariable("id") Long id,
            @RequestBody DoctorUpdateReq req) {
        DoctorVO vo = doctorService.updateDoctor(id, req);
        return Result.success(vo);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询医生", description = "支持按科室和关键字组合筛选医生列表。")
    public Result<PageResult<DoctorVO>> getDoctorsPage(
            @Parameter(description = "页码，从 1 开始") @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @Parameter(description = "每页条数") @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @Parameter(description = "科室 ID，可选") @RequestParam(value = "departmentId", required = false) Long departmentId,
            @Parameter(description = "医生姓名或工号关键字，可选") @RequestParam(value = "keyword", required = false) String keyword) {
        Page<DoctorVO> page = doctorService.getDoctorsPage(pageNo, pageSize, departmentId, keyword);
        return Result.success(new PageResult<>(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal()));
    }

    @GetMapping
    @Operation(summary = "按科室查询医生列表", description = "不分页返回医生列表，可按科室过滤。")
    public Result<List<DoctorVO>> getDoctors(
            @Parameter(description = "科室 ID，可选") @RequestParam(value = "departmentId", required = false) Long departmentId) {
        List<DoctorVO> list = doctorService.getDoctorsByDepartment(departmentId);
        return Result.success(list);
    }
}
