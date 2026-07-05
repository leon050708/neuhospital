package com.neusoft.neu23.neuhospital.registration.service;

import com.neusoft.neu23.neuhospital.registration.dto.ScheduleBatchGenerateReq;
import com.neusoft.neu23.neuhospital.registration.vo.ScheduleGenerateResultVO;

public interface ScheduleGenerateService {
    ScheduleGenerateResultVO generateFromTemplates(ScheduleBatchGenerateReq req);
}
