package com.neusoft.neu23.neuhospital.registration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.doctor.entity.DepartmentEntity;
import com.neusoft.neu23.neuhospital.doctor.entity.DoctorEntity;
import com.neusoft.neu23.neuhospital.doctor.mapper.DepartmentMapper;
import com.neusoft.neu23.neuhospital.doctor.mapper.DoctorMapper;
import com.neusoft.neu23.neuhospital.registration.dto.DoctorScheduleCreateReq;
import com.neusoft.neu23.neuhospital.registration.dto.DoctorScheduleUpdateReq;
import com.neusoft.neu23.neuhospital.registration.entity.DoctorScheduleEntity;
import com.neusoft.neu23.neuhospital.registration.mapper.DoctorScheduleMapper;
import com.neusoft.neu23.neuhospital.registration.service.DoctorScheduleService;
import com.neusoft.neu23.neuhospital.registration.vo.DoctorScheduleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DoctorScheduleServiceImpl implements DoctorScheduleService {

    private final DoctorScheduleMapper doctorScheduleMapper;
    private final DoctorMapper doctorMapper;
    private final DepartmentMapper departmentMapper;

    public DoctorScheduleServiceImpl(DoctorScheduleMapper doctorScheduleMapper, DoctorMapper doctorMapper, DepartmentMapper departmentMapper) {
        this.doctorScheduleMapper = doctorScheduleMapper;
        this.doctorMapper = doctorMapper;
        this.departmentMapper = departmentMapper;
    }

    @Override
    @Transactional
    public DoctorScheduleVO createSchedule(DoctorScheduleCreateReq req) {
        // 校验医生和科室
        DoctorEntity doctor = doctorMapper.selectById(req.getDoctorId());
        if (doctor == null || !"ENABLED".equals(doctor.getStatus())) {
            throw new BusinessException("医生不存在或已停诊");
        }
        DepartmentEntity dept = departmentMapper.selectById(req.getDepartmentId());
        if (dept == null || !"ENABLED".equals(dept.getStatus())) {
            throw new BusinessException("科室不存在或已停用");
        }

        // 防重校验：同一天同时段
        Long count = doctorScheduleMapper.selectCount(new QueryWrapper<DoctorScheduleEntity>()
                .eq("doctor_id", req.getDoctorId())
                .eq("schedule_date", req.getScheduleDate())
                .eq("time_slot", req.getTimeSlot()));
        if (count > 0) {
            throw new BusinessException("该医生在此时间段已有排班");
        }

        DoctorScheduleEntity entity = new DoctorScheduleEntity();
        entity.setDoctorId(req.getDoctorId());
        entity.setDepartmentId(req.getDepartmentId());
        entity.setScheduleDate(req.getScheduleDate());
        entity.setTimeSlot(req.getTimeSlot());
        entity.setSourceCount(req.getSourceCount() != null ? req.getSourceCount() : 0);
        entity.setAvailableCount(entity.getSourceCount()); // 初始可用等于总数
        entity.setFeeAmount(req.getFeeAmount());
        entity.setSourceType(req.getSourceType());
        entity.setStatus("ENABLED");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        doctorScheduleMapper.insert(entity);
        return convertToVO(entity, doctor.getName(), dept.getDeptName());
    }

    @Override
    @Transactional
    public DoctorScheduleVO updateSchedule(Long id, DoctorScheduleUpdateReq req) {
        DoctorScheduleEntity entity = doctorScheduleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("排班记录不存在");
        }

        // 简单处理：如果有已挂号的，修改总号源不能小于已经挂出去的号源
        if (req.getSourceCount() != null) {
            int usedCount = entity.getSourceCount() - entity.getAvailableCount();
            if (req.getSourceCount() < usedCount) {
                throw new BusinessException("总号源数不能小于已挂出的号源数");
            }
            entity.setSourceCount(req.getSourceCount());
            entity.setAvailableCount(req.getSourceCount() - usedCount);
        }

        if (req.getFeeAmount() != null) entity.setFeeAmount(req.getFeeAmount());
        if (req.getSourceType() != null) entity.setSourceType(req.getSourceType());
        if (req.getStatus() != null) entity.setStatus(req.getStatus());

        entity.setUpdatedAt(LocalDateTime.now());
        doctorScheduleMapper.updateById(entity);

        return fetchVO(entity);
    }

    @Override
    public void closeSchedule(Long id) {
        DoctorScheduleEntity entity = doctorScheduleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("排班记录不存在");
        }
        entity.setStatus("CLOSED");
        entity.setUpdatedAt(LocalDateTime.now());
        doctorScheduleMapper.updateById(entity);
    }

    @Override
    public Page<DoctorScheduleVO> getSchedulesPage(Integer pageNo, Integer pageSize, Long doctorId, Long departmentId, LocalDate scheduleDate, String timeSlot) {
        Page<DoctorScheduleEntity> page = new Page<>(pageNo != null ? pageNo : 1, pageSize != null ? pageSize : 10);
        QueryWrapper<DoctorScheduleEntity> wrapper = new QueryWrapper<>();
        if (doctorId != null) wrapper.eq("doctor_id", doctorId);
        if (departmentId != null) wrapper.eq("department_id", departmentId);
        if (scheduleDate != null) wrapper.eq("schedule_date", scheduleDate);
        if (StringUtils.hasText(timeSlot)) wrapper.eq("time_slot", timeSlot);
        wrapper.orderByDesc("schedule_date");

        doctorScheduleMapper.selectPage(page, wrapper);

        Map<Long, DoctorEntity> doctorsById = loadDoctors(page.getRecords());
        Map<Long, DepartmentEntity> departmentsById = loadDepartments(page.getRecords());
        List<DoctorScheduleVO> voList = page.getRecords().stream()
                .map(record -> convertToVO(
                        record,
                        doctorsById.get(record.getDoctorId()) != null ? doctorsById.get(record.getDoctorId()).getName() : "",
                        departmentsById.get(record.getDepartmentId()) != null ? departmentsById.get(record.getDepartmentId()).getDeptName() : ""))
                .collect(Collectors.toList());

        Page<DoctorScheduleVO> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    private DoctorScheduleVO fetchVO(DoctorScheduleEntity entity) {
        DoctorEntity doctor = doctorMapper.selectById(entity.getDoctorId());
        DepartmentEntity dept = departmentMapper.selectById(entity.getDepartmentId());
        return convertToVO(entity, 
                doctor != null ? doctor.getName() : "", 
                dept != null ? dept.getDeptName() : "");
    }

    private Map<Long, DoctorEntity> loadDoctors(List<DoctorScheduleEntity> schedules) {
        Set<Long> doctorIds = schedules.stream()
                .map(DoctorScheduleEntity::getDoctorId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (doctorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return doctorMapper.selectBatchIds(doctorIds).stream()
                .collect(Collectors.toMap(DoctorEntity::getId, Function.identity()));
    }

    private Map<Long, DepartmentEntity> loadDepartments(List<DoctorScheduleEntity> schedules) {
        Set<Long> departmentIds = schedules.stream()
                .map(DoctorScheduleEntity::getDepartmentId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (departmentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return departmentMapper.selectBatchIds(departmentIds).stream()
                .collect(Collectors.toMap(DepartmentEntity::getId, Function.identity()));
    }

    private DoctorScheduleVO convertToVO(DoctorScheduleEntity entity, String doctorName, String deptName) {
        DoctorScheduleVO vo = new DoctorScheduleVO();
        vo.setId(entity.getId());
        vo.setDoctorId(entity.getDoctorId());
        vo.setDoctorName(doctorName);
        vo.setDepartmentId(entity.getDepartmentId());
        vo.setDepartmentName(deptName);
        vo.setScheduleDate(entity.getScheduleDate());
        vo.setTimeSlot(entity.getTimeSlot());
        vo.setSourceCount(entity.getSourceCount());
        vo.setAvailableCount(entity.getAvailableCount());
        vo.setFeeAmount(entity.getFeeAmount());
        vo.setSourceType(entity.getSourceType());
        vo.setStatus(entity.getStatus());
        return vo;
    }
}
