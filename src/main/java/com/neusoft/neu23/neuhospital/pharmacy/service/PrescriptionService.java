package com.neusoft.neu23.neuhospital.pharmacy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.neu23.neuhospital.pharmacy.dto.PrescriptionCreateReq;
import com.neusoft.neu23.neuhospital.pharmacy.entity.PrescriptionEntity;
import com.neusoft.neu23.neuhospital.pharmacy.vo.PrescriptionVO;

public interface PrescriptionService extends IService<PrescriptionEntity> {
    Long createPrescription(PrescriptionCreateReq req);
    PrescriptionVO getPrescriptionDetail(Long id);
    Page<PrescriptionVO> getPrescriptionsPage(Integer pageNo, Integer pageSize, Long patientId, Long doctorId);
}
