package com.neusoft.neu23.neuhospital.outpatient.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.neu23.neuhospital.outpatient.dto.MedicalDiagnosisReq;
import com.neusoft.neu23.neuhospital.outpatient.dto.MedicalRecordCreateReq;
import com.neusoft.neu23.neuhospital.outpatient.dto.MedicalRecordUpdateReq;
import com.neusoft.neu23.neuhospital.outpatient.entity.MedicalRecordEntity;
import com.neusoft.neu23.neuhospital.outpatient.vo.MedicalDiagnosisVO;
import com.neusoft.neu23.neuhospital.outpatient.vo.MedicalRecordVO;

import java.util.List;

public interface MedicalRecordService extends IService<MedicalRecordEntity> {
    
    Long createRecord(MedicalRecordCreateReq req);
    
    void updateRecord(Long id, MedicalRecordUpdateReq req);
    
    void confirmRecord(Long id);
    
    MedicalRecordVO getRecordDetail(Long id);
    
    Page<MedicalRecordVO> getRecordsPage(Integer pageNo, Integer pageSize, Long patientId, Long doctorId);
    
    void saveDiagnoses(Long recordId, List<MedicalDiagnosisReq> diagnoses);
    
    List<MedicalDiagnosisVO> getDiagnosesByRecordId(Long recordId);
}
