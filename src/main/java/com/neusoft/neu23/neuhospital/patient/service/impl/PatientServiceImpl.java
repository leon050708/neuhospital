package com.neusoft.neu23.neuhospital.patient.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.patient.dto.PatientCreateReq;
import com.neusoft.neu23.neuhospital.patient.dto.PatientUpdateReq;
import com.neusoft.neu23.neuhospital.patient.entity.PatientEntity;
import com.neusoft.neu23.neuhospital.patient.mapper.PatientMapper;
import com.neusoft.neu23.neuhospital.patient.service.PatientService;
import com.neusoft.neu23.neuhospital.patient.vo.PatientVO;
import com.neusoft.neu23.neuhospital.system.entity.SysUserEntity;
import com.neusoft.neu23.neuhospital.system.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientMapper patientMapper;
    private final SysUserMapper sysUserMapper;

    public PatientServiceImpl(PatientMapper patientMapper, SysUserMapper sysUserMapper) {
        this.patientMapper = patientMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    @Transactional
    public PatientVO createPatient(PatientCreateReq req) {
        // 1. 防重校验
        if (req.getIdCard() != null) {
            Long count = patientMapper.selectCount(new QueryWrapper<PatientEntity>().eq("id_card", req.getIdCard()));
            if (count > 0) {
                throw new BusinessException("该身份证号已建档，无法重复创建");
            }
        }
        if (req.getPhone() != null) {
            Long count = patientMapper.selectCount(new QueryWrapper<PatientEntity>().eq("phone", req.getPhone()));
            if (count > 0) {
                throw new BusinessException("该手机号已建档，无法重复创建");
            }
        }

        // 2. 生成编号
        String patientNo = "PAT-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));

        // 3. 构建实体并保存
        PatientEntity entity = new PatientEntity();
        entity.setPatientNo(patientNo);
        entity.setName(req.getName());
        entity.setGender(req.getGender());
        entity.setBirthDate(req.getBirthDate());
        entity.setPhone(req.getPhone());
        entity.setIdCard(req.getIdCard());
        entity.setBloodType(req.getBloodType());
        entity.setAllergySummary(req.getAllergySummary());
        entity.setHistorySummary(req.getHistorySummary());
        entity.setEmergencyContact(req.getEmergencyContact());
        entity.setEmergencyPhone(req.getEmergencyPhone());
        entity.setStatus("ENABLED");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        patientMapper.insert(entity);

        // 4. 联动创建 SysUser 登录账户
        SysUserEntity user = new SysUserEntity();
        user.setUsername(req.getPhone()); // 默认使用手机号作为登录名
        user.setPasswordHash("$2a$10$xyz123"); // 初始密码加密，这里简单写死，实际应调用 PasswordEncoder
        user.setUserType("PATIENT");
        user.setBizId(entity.getId());
        user.setRealName(entity.getName());
        user.setPhone(entity.getPhone());
        user.setStatus("ENABLED");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.insert(user);

        // TODO: 可选，向 sys_user_role 表中插入关联角色(PATIENT)

        // 5. 封装返回值
        PatientVO vo = new PatientVO();
        return convertToVO(entity);
    }

    @Override
    public PatientVO getPatientById(Long id) {
        PatientEntity entity = patientMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("患者不存在");
        }
        return convertToVO(entity);
    }

    @Override
    @Transactional
    public PatientVO updatePatient(Long id, PatientUpdateReq req) {
        PatientEntity entity = patientMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("患者不存在");
        }
        if (StringUtils.hasText(req.getName())) entity.setName(req.getName());
        if (StringUtils.hasText(req.getPhone())) {
            Long count = patientMapper.selectCount(new QueryWrapper<PatientEntity>().eq("phone", req.getPhone()).ne("id", id));
            if (count > 0) throw new BusinessException("该手机号已被其他患者绑定");
            entity.setPhone(req.getPhone());
        }
        if (req.getBloodType() != null) entity.setBloodType(req.getBloodType());
        if (req.getAllergySummary() != null) entity.setAllergySummary(req.getAllergySummary());
        if (req.getHistorySummary() != null) entity.setHistorySummary(req.getHistorySummary());
        if (req.getEmergencyContact() != null) entity.setEmergencyContact(req.getEmergencyContact());
        if (req.getEmergencyPhone() != null) entity.setEmergencyPhone(req.getEmergencyPhone());
        if (StringUtils.hasText(req.getStatus())) entity.setStatus(req.getStatus());
        
        entity.setUpdatedAt(LocalDateTime.now());
        patientMapper.updateById(entity);
        return convertToVO(entity);
    }

    @Override
    public Page<PatientVO> getPatientsPage(Integer pageNo, Integer pageSize, String keyword) {
        Page<PatientEntity> page = new Page<>(pageNo != null ? pageNo : 1, pageSize != null ? pageSize : 10);
        QueryWrapper<PatientEntity> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like("name", keyword).or().like("phone", keyword).or().like("id_card", keyword);
        }
        wrapper.orderByDesc("created_at");
        patientMapper.selectPage(page, wrapper);

        List<PatientVO> voList = page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList());
        Page<PatientVO> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    private PatientVO convertToVO(PatientEntity entity) {
        PatientVO vo = new PatientVO();
        vo.setId(entity.getId());
        vo.setPatientNo(entity.getPatientNo());
        vo.setName(entity.getName());
        vo.setGender(entity.getGender());
        vo.setBirthDate(entity.getBirthDate());
        vo.setPhone(entity.getPhone());
        vo.setIdCard(entity.getIdCard());
        vo.setBloodType(entity.getBloodType());
        vo.setAllergySummary(entity.getAllergySummary());
        vo.setHistorySummary(entity.getHistorySummary());
        vo.setStatus(entity.getStatus());
        return vo;
    }}
