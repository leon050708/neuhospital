package com.neusoft.neu23.neuhospital.registration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.registration.config.RegistrationScheduleProperties;
import com.neusoft.neu23.neuhospital.registration.dto.DoctorScheduleCreateReq;
import com.neusoft.neu23.neuhospital.registration.dto.ScheduleBatchGenerateReq;
import com.neusoft.neu23.neuhospital.registration.entity.DoctorScheduleEntity;
import com.neusoft.neu23.neuhospital.registration.entity.DoctorScheduleTemplateEntity;
import com.neusoft.neu23.neuhospital.registration.mapper.DoctorScheduleMapper;
import com.neusoft.neu23.neuhospital.registration.mapper.DoctorScheduleTemplateMapper;
import com.neusoft.neu23.neuhospital.registration.service.DoctorScheduleService;
import com.neusoft.neu23.neuhospital.registration.service.ScheduleGenerateService;
import com.neusoft.neu23.neuhospital.registration.vo.DoctorScheduleVO;
import com.neusoft.neu23.neuhospital.registration.vo.ScheduleGenerateResultVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
public class ScheduleGenerateServiceImpl implements ScheduleGenerateService {

    private final DoctorScheduleTemplateMapper templateMapper;
    private final DoctorScheduleMapper doctorScheduleMapper;
    private final DoctorScheduleService doctorScheduleService;
    private final RegistrationScheduleProperties scheduleProperties;

    public ScheduleGenerateServiceImpl(DoctorScheduleTemplateMapper templateMapper,
                                       DoctorScheduleMapper doctorScheduleMapper,
                                       DoctorScheduleService doctorScheduleService,
                                       RegistrationScheduleProperties scheduleProperties) {
        this.templateMapper = templateMapper;
        this.doctorScheduleMapper = doctorScheduleMapper;
        this.doctorScheduleService = doctorScheduleService;
        this.scheduleProperties = scheduleProperties;
    }

    @Override
    @Transactional
    public ScheduleGenerateResultVO generateFromTemplates(ScheduleBatchGenerateReq req) {
        int days = req.getDays() != null && req.getDays() > 0 ? req.getDays() : scheduleProperties.getAdvanceDays();
        LocalDate startDate = req.getStartDate() != null ? req.getStartDate() : LocalDate.now();
        LocalDate endDate = startDate.plusDays(days - 1L);

        QueryWrapper<DoctorScheduleTemplateEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "ENABLED").eq("deleted", false);
        if (req.getDoctorId() != null) {
            wrapper.eq("doctor_id", req.getDoctorId());
        }
        if (req.getDepartmentId() != null) {
            wrapper.eq("department_id", req.getDepartmentId());
        }
        List<DoctorScheduleTemplateEntity> templates = templateMapper.selectList(wrapper);
        if (templates.isEmpty()) {
            throw new BusinessException("没有可用的排班模板，请先创建模板");
        }

        ScheduleGenerateResultVO result = new ScheduleGenerateResultVO();
        result.setStartDate(startDate);
        result.setEndDate(endDate);

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            int dayOfWeek = date.getDayOfWeek().getValue();
            for (DoctorScheduleTemplateEntity template : templates) {
                if (!Integer.valueOf(dayOfWeek).equals(template.getDayOfWeek())) {
                    continue;
                }
                if (scheduleExists(template.getDoctorId(), date, template.getTimeSlot())) {
                    result.setSkippedCount(result.getSkippedCount() + 1);
                    continue;
                }
                DoctorScheduleCreateReq createReq = new DoctorScheduleCreateReq();
                createReq.setDoctorId(template.getDoctorId());
                createReq.setDepartmentId(template.getDepartmentId());
                createReq.setScheduleDate(date);
                createReq.setTimeSlot(template.getTimeSlot());
                createReq.setSourceCount(template.getSourceCount());
                createReq.setFeeAmount(template.getFeeAmount());
                createReq.setSourceType(template.getSourceType());
                DoctorScheduleVO created = doctorScheduleService.createSchedule(createReq);
                result.setCreatedCount(result.getCreatedCount() + 1);
                result.getCreatedScheduleIds().add(created.getId());
            }
        }
        return result;
    }

    private boolean scheduleExists(Long doctorId, LocalDate scheduleDate, String timeSlot) {
        Long count = doctorScheduleMapper.selectCount(new QueryWrapper<DoctorScheduleEntity>()
                .eq("doctor_id", doctorId)
                .eq("schedule_date", scheduleDate)
                .eq("time_slot", timeSlot)
                .eq("deleted", false));
        return count != null && count > 0;
    }
}
