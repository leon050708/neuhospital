package com.neusoft.neu23.neuhospital.doctor.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.doctor.dto.DoctorCreateReq;
import com.neusoft.neu23.neuhospital.doctor.dto.DoctorUpdateReq;
import com.neusoft.neu23.neuhospital.doctor.vo.DoctorVO;

import java.util.List;

public interface DoctorService {
    DoctorVO createDoctor(DoctorCreateReq req);
    DoctorVO getDoctorById(Long id);
    DoctorVO updateDoctor(Long id, DoctorUpdateReq req);
    Page<DoctorVO> getDoctorsPage(Integer pageNo, Integer pageSize, Long departmentId, String keyword);
    List<DoctorVO> getDoctorsByDepartment(Long departmentId);
}
