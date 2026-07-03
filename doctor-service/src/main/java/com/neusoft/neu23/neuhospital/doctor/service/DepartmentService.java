package com.neusoft.neu23.neuhospital.doctor.service;

import com.neusoft.neu23.neuhospital.doctor.dto.DepartmentCreateReq;
import com.neusoft.neu23.neuhospital.doctor.dto.DepartmentUpdateReq;
import com.neusoft.neu23.neuhospital.doctor.vo.DepartmentVO;

import java.util.List;

public interface DepartmentService {
    DepartmentVO createDepartment(DepartmentCreateReq req);
    DepartmentVO getDepartmentById(Long id);
    DepartmentVO updateDepartment(Long id, DepartmentUpdateReq req);
    List<DepartmentVO> getAllDepartments();
}
