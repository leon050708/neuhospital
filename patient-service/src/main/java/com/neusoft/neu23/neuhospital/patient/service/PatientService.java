package com.neusoft.neu23.neuhospital.patient.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.patient.dto.PatientCreateReq;
import com.neusoft.neu23.neuhospital.patient.dto.PatientUpdateReq;
import com.neusoft.neu23.neuhospital.patient.vo.PatientVO;

public interface PatientService {
    
    /**
     * 创建患者档案，并联动创建系统登录账号
     */
    PatientVO createPatient(PatientCreateReq req);

    /**
     * 查询患者详情
     */
    PatientVO getPatientById(Long id);

    /**
     * 修改患者信息
     */
    PatientVO updatePatient(Long id, PatientUpdateReq req);

    /**
     * 分页查询患者列表
     */
    Page<PatientVO> getPatientsPage(Integer pageNo, Integer pageSize, String keyword);
}

