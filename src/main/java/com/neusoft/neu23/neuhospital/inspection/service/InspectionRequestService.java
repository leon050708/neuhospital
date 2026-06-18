package com.neusoft.neu23.neuhospital.inspection.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.neu23.neuhospital.inspection.dto.InspectionRequestCreateReq;
import com.neusoft.neu23.neuhospital.inspection.entity.InspectionRequestEntity;
import com.neusoft.neu23.neuhospital.inspection.vo.InspectionRequestVO;

public interface InspectionRequestService extends IService<InspectionRequestEntity> {
    Long createRequest(InspectionRequestCreateReq req);
    InspectionRequestVO getRequestDetail(Long id);
    Page<InspectionRequestVO> getRequestsPage(Integer pageNo, Integer pageSize, Long patientId, Long doctorId);
}
