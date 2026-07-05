package com.neusoft.neu23.neuhospital.registration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.doctor.entity.DepartmentEntity;
import com.neusoft.neu23.neuhospital.doctor.entity.DoctorEntity;
import com.neusoft.neu23.neuhospital.doctor.mapper.DepartmentMapper;
import com.neusoft.neu23.neuhospital.doctor.mapper.DoctorMapper;
import com.neusoft.neu23.neuhospital.registration.dto.ScheduleTemplateCreateReq;
import com.neusoft.neu23.neuhospital.registration.dto.ScheduleTemplateUpdateReq;
import com.neusoft.neu23.neuhospital.registration.entity.DoctorScheduleTemplateEntity;
import com.neusoft.neu23.neuhospital.registration.mapper.DoctorScheduleTemplateMapper;
import com.neusoft.neu23.neuhospital.registration.service.DoctorScheduleTemplateService;
import com.neusoft.neu23.neuhospital.registration.vo.ScheduleTemplateVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DoctorScheduleTemplateServiceImpl implements DoctorScheduleTemplateService {

    private final DoctorScheduleTemplateMapper templateMapper;
    private final DoctorMapper doctorMapper;
    private final DepartmentMapper departmentMapper;

    public DoctorScheduleTemplateServiceImpl(DoctorScheduleTemplateMapper templateMapper,
                                             DoctorMapper doctorMapper,
                                             DepartmentMapper departmentMapper) {
        this.templateMapper = templateMapper;
        this.doctorMapper = doctorMapper;
        this.departmentMapper = departmentMapper;
    }

    @Override
    @Transactional
    public ScheduleTemplateVO createTemplate(ScheduleTemplateCreateReq req) {
        validateTemplateRequest(req.getDoctorId(), req.getDepartmentId(), req.getDayOfWeek(), req.getTimeSlot(), req.getSourceCount(), req.getFeeAmount());

        Long duplicate = templateMapper.selectCount(new QueryWrapper<DoctorScheduleTemplateEntity>()
                .eq("doctor_id", req.getDoctorId())
                .eq("day_of_week", req.getDayOfWeek())
                .eq("time_slot", req.getTimeSlot())
                .eq("deleted", false));
        if (duplicate != null && duplicate > 0) {
            throw new BusinessException("该医生在此星期时段已有排班模板");
        }

        DoctorScheduleTemplateEntity entity = new DoctorScheduleTemplateEntity();
        entity.setDoctorId(req.getDoctorId());
        entity.setDepartmentId(req.getDepartmentId());
        entity.setDayOfWeek(req.getDayOfWeek());
        entity.setTimeSlot(req.getTimeSlot());
        entity.setSourceCount(req.getSourceCount());
        entity.setFeeAmount(req.getFeeAmount());
        entity.setSourceType(req.getSourceType() != null ? req.getSourceType() : "NORMAL");
        entity.setStatus("ENABLED");
        entity.setDeleted(false);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        templateMapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public ScheduleTemplateVO updateTemplate(Long id, ScheduleTemplateUpdateReq req) {
        DoctorScheduleTemplateEntity entity = requireTemplate(id);
        if (req.getSourceCount() != null) {
            if (req.getSourceCount() <= 0) {
                throw new BusinessException("号源数必须大于 0");
            }
            entity.setSourceCount(req.getSourceCount());
        }
        if (req.getFeeAmount() != null) {
            entity.setFeeAmount(req.getFeeAmount());
        }
        if (req.getSourceType() != null) {
            entity.setSourceType(req.getSourceType());
        }
        if (req.getStatus() != null) {
            entity.setStatus(req.getStatus());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public void disableTemplate(Long id) {
        DoctorScheduleTemplateEntity entity = requireTemplate(id);
        entity.setStatus("DISABLED");
        entity.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(entity);
    }

    @Override
    public ScheduleTemplateVO getTemplate(Long id) {
        return toVO(requireTemplate(id));
    }

    @Override
    public Page<ScheduleTemplateVO> listTemplates(Integer pageNo, Integer pageSize, Long doctorId, Long departmentId) {
        Page<DoctorScheduleTemplateEntity> page = new Page<>(pageNo != null ? pageNo : 1, pageSize != null ? pageSize : 10);
        QueryWrapper<DoctorScheduleTemplateEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", false);
        if (doctorId != null) {
            wrapper.eq("doctor_id", doctorId);
        }
        if (departmentId != null) {
            wrapper.eq("department_id", departmentId);
        }
        wrapper.orderByAsc("doctor_id", "day_of_week", "time_slot");
        templateMapper.selectPage(page, wrapper);

        Page<ScheduleTemplateVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return result;
    }

    private DoctorScheduleTemplateEntity requireTemplate(Long id) {
        DoctorScheduleTemplateEntity entity = templateMapper.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getDeleted())) {
            throw new BusinessException("排班模板不存在");
        }
        return entity;
    }

    private void validateTemplateRequest(Long doctorId, Long departmentId, Integer dayOfWeek,
                                         String timeSlot, Integer sourceCount, java.math.BigDecimal feeAmount) {
        DoctorEntity doctor = doctorMapper.selectById(doctorId);
        if (doctor == null || !"ENABLED".equals(doctor.getStatus())) {
            throw new BusinessException("医生不存在或已停诊");
        }
        DepartmentEntity dept = departmentMapper.selectById(departmentId);
        if (dept == null || !"ENABLED".equals(dept.getStatus())) {
            throw new BusinessException("科室不存在或已停用");
        }
        if (dayOfWeek == null || dayOfWeek < DayOfWeek.MONDAY.getValue() || dayOfWeek > DayOfWeek.SUNDAY.getValue()) {
            throw new BusinessException("dayOfWeek 必须为 1(周一) 到 7(周日)");
        }
        if (timeSlot == null || timeSlot.isBlank()) {
            throw new BusinessException("时段不能为空");
        }
        if (sourceCount == null || sourceCount <= 0) {
            throw new BusinessException("号源数必须大于 0");
        }
        if (feeAmount == null) {
            throw new BusinessException("挂号费不能为空");
        }
    }

    private ScheduleTemplateVO toVO(DoctorScheduleTemplateEntity entity) {
        DoctorEntity doctor = doctorMapper.selectById(entity.getDoctorId());
        DepartmentEntity dept = departmentMapper.selectById(entity.getDepartmentId());
        ScheduleTemplateVO vo = new ScheduleTemplateVO();
        vo.setId(entity.getId());
        vo.setDoctorId(entity.getDoctorId());
        vo.setDoctorName(doctor != null ? doctor.getName() : "");
        vo.setDepartmentId(entity.getDepartmentId());
        vo.setDepartmentName(dept != null ? dept.getDeptName() : "");
        vo.setDayOfWeek(entity.getDayOfWeek());
        vo.setTimeSlot(entity.getTimeSlot());
        vo.setSourceCount(entity.getSourceCount());
        vo.setFeeAmount(entity.getFeeAmount());
        vo.setSourceType(entity.getSourceType());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
