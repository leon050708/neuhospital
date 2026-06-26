package com.neusoft.neu23.neuhospital.inspection.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.neu23.neuhospital.inspection.dto.DisposalRequestCreateReq;
import com.neusoft.neu23.neuhospital.inspection.entity.DisposalRequestEntity;
import com.neusoft.neu23.neuhospital.inspection.vo.DisposalRequestVO;

public interface DisposalRequestService extends IService<DisposalRequestEntity> {
    Long createRequest(DisposalRequestCreateReq req);
    DisposalRequestVO getRequestDetail(Long id);
    Page<DisposalRequestVO> getRequestsPage(Integer pageNo, Integer pageSize, Long patientId, Long doctorId);
    void cancelRequest(Long id);
    void finishRequest(Long id);
}
