package com.neusoft.neu23.neuhospital.inspection.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.neu23.neuhospital.inspection.dto.CheckRequestCreateReq;
import com.neusoft.neu23.neuhospital.inspection.entity.CheckRequestEntity;
import com.neusoft.neu23.neuhospital.inspection.vo.CheckRequestVO;

public interface CheckRequestService extends IService<CheckRequestEntity> {
    Long createRequest(CheckRequestCreateReq req);
    CheckRequestVO getRequestDetail(Long id);
    Page<CheckRequestVO> getRequestsPage(Integer pageNo, Integer pageSize, Long patientId, Long doctorId);
    void cancelRequest(Long id);
}
