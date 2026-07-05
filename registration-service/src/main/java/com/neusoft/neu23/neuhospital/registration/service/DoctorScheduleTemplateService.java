package com.neusoft.neu23.neuhospital.registration.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.registration.dto.ScheduleTemplateCreateReq;
import com.neusoft.neu23.neuhospital.registration.dto.ScheduleTemplateUpdateReq;
import com.neusoft.neu23.neuhospital.registration.vo.ScheduleTemplateVO;

public interface DoctorScheduleTemplateService {
    ScheduleTemplateVO createTemplate(ScheduleTemplateCreateReq req);

    ScheduleTemplateVO updateTemplate(Long id, ScheduleTemplateUpdateReq req);

    void disableTemplate(Long id);

    ScheduleTemplateVO getTemplate(Long id);

    Page<ScheduleTemplateVO> listTemplates(Integer pageNo, Integer pageSize, Long doctorId, Long departmentId);
}
