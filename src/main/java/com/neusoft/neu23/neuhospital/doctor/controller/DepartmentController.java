package com.neusoft.neu23.neuhospital.doctor.controller;

import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.doctor.dto.DepartmentCreateReq;
import com.neusoft.neu23.neuhospital.doctor.dto.DepartmentUpdateReq;
import com.neusoft.neu23.neuhospital.doctor.service.DepartmentService;
import com.neusoft.neu23.neuhospital.doctor.vo.DepartmentVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    public Result<DepartmentVO> createDepartment(@RequestBody DepartmentCreateReq req) {
        DepartmentVO vo = departmentService.createDepartment(req);
        return Result.success(vo);
    }

    @GetMapping("/{id}")
    public Result<DepartmentVO> getDepartment(@PathVariable("id") Long id) {
        DepartmentVO vo = departmentService.getDepartmentById(id);
        return Result.success(vo);
    }

    @PutMapping("/{id}")
    public Result<DepartmentVO> updateDepartment(@PathVariable("id") Long id, @RequestBody DepartmentUpdateReq req) {
        DepartmentVO vo = departmentService.updateDepartment(id, req);
        return Result.success(vo);
    }

    @GetMapping
    public Result<List<DepartmentVO>> getAllDepartments() {
        List<DepartmentVO> list = departmentService.getAllDepartments();
        return Result.success(list);
    }

    // 伪树接口，直接返回平铺列表
    @GetMapping("/tree")
    public Result<List<DepartmentVO>> getDepartmentTree() {
        List<DepartmentVO> list = departmentService.getAllDepartments();
        return Result.success(list);
    }
}
