package com.neusoft.neu23.neuhospital.inspection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.neu23.neuhospital.inspection.dto.CheckResultCreateReq;
import com.neusoft.neu23.neuhospital.inspection.entity.CheckResultEntity;
import com.neusoft.neu23.neuhospital.inspection.vo.CheckResultVO;

public interface CheckResultService extends IService<CheckResultEntity> {
    Long recordResult(CheckResultCreateReq req);
    CheckResultVO getResultDetail(Long id);
    void confirmResult(Long id);
}
