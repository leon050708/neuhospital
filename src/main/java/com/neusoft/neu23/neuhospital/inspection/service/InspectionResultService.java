package com.neusoft.neu23.neuhospital.inspection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.neu23.neuhospital.inspection.dto.InspectionResultCreateReq;
import com.neusoft.neu23.neuhospital.inspection.entity.InspectionResultEntity;
import com.neusoft.neu23.neuhospital.inspection.vo.InspectionResultVO;

public interface InspectionResultService extends IService<InspectionResultEntity> {
    Long recordResult(InspectionResultCreateReq req);
    InspectionResultVO getResultDetail(Long id);
}
