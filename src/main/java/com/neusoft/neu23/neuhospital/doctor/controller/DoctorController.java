package com.neusoft.neu23.neuhospital.doctor.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.common.response.PageResult;
import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.doctor.dto.DoctorCreateReq;
import com.neusoft.neu23.neuhospital.doctor.dto.DoctorUpdateReq;
import com.neusoft.neu23.neuhospital.doctor.service.DoctorService;
import com.neusoft.neu23.neuhospital.doctor.vo.DoctorVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @PostMapping
    public Result<DoctorVO> createDoctor(@RequestBody DoctorCreateReq req) {
        DoctorVO vo = doctorService.createDoctor(req);
        return Result.success(vo);
    }

    @GetMapping("/{id}")
    public Result<DoctorVO> getDoctor(@PathVariable("id") Long id) {
        DoctorVO vo = doctorService.getDoctorById(id);
        return Result.success(vo);
    }

    @PutMapping("/{id}")
    public Result<DoctorVO> updateDoctor(@PathVariable("id") Long id, @RequestBody DoctorUpdateReq req) {
        DoctorVO vo = doctorService.updateDoctor(id, req);
        return Result.success(vo);
    }

    @GetMapping("/page")
    public Result<PageResult<DoctorVO>> getDoctorsPage(
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "keyword", required = false) String keyword) {
        Page<DoctorVO> page = doctorService.getDoctorsPage(pageNo, pageSize, departmentId, keyword);
        return Result.success(new PageResult<>(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal()));
    }

    @GetMapping
    public Result<List<DoctorVO>> getDoctors(@RequestParam(value = "departmentId", required = false) Long departmentId) {
        List<DoctorVO> list = doctorService.getDoctorsByDepartment(departmentId);
        return Result.success(list);
    }
}
