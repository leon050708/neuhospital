package com.neusoft.neu23.neuhospital.ct.service;

import com.neusoft.neu23.neuhospital.ct.dto.CtAnalysisTaskCreateReq;
import com.neusoft.neu23.neuhospital.ct.vo.CtAnalysisResultVO;
import com.neusoft.neu23.neuhospital.ct.vo.CtAnalysisTaskVO;

public interface CtAnalysisService {

    CtAnalysisTaskVO createTask(CtAnalysisTaskCreateReq req);

    CtAnalysisResultVO getResult(Long taskId);
}
