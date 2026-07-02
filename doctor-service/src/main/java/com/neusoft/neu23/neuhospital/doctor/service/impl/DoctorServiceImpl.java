package com.neusoft.neu23.neuhospital.doctor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.doctor.dto.DoctorCreateReq;
import com.neusoft.neu23.neuhospital.doctor.dto.DoctorUpdateReq;
import com.neusoft.neu23.neuhospital.doctor.entity.DepartmentEntity;
import com.neusoft.neu23.neuhospital.doctor.entity.DoctorEntity;
import com.neusoft.neu23.neuhospital.doctor.mapper.DepartmentMapper;
import com.neusoft.neu23.neuhospital.doctor.mapper.DoctorMapper;
import com.neusoft.neu23.neuhospital.doctor.service.DoctorService;
import com.neusoft.neu23.neuhospital.doctor.vo.DoctorVO;
import com.neusoft.neu23.neuhospital.system.entity.SysUserEntity;
import com.neusoft.neu23.neuhospital.system.mapper.SysUserMapper;
import com.neusoft.neu23.neuhospital.system.service.SysUserRoleService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DoctorServiceImpl implements DoctorService {

    private final DoctorMapper doctorMapper;
    private final DepartmentMapper departmentMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleService sysUserRoleService;
    private final PasswordEncoder passwordEncoder;

    public DoctorServiceImpl(DoctorMapper doctorMapper,
                             DepartmentMapper departmentMapper,
                             SysUserMapper sysUserMapper,
                             SysUserRoleService sysUserRoleService,
                             PasswordEncoder passwordEncoder) {
        this.doctorMapper = doctorMapper;
        this.departmentMapper = departmentMapper;
        this.sysUserMapper = sysUserMapper;
        this.sysUserRoleService = sysUserRoleService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public DoctorVO createDoctor(DoctorCreateReq req) {
        // 1. 校验科室
        DepartmentEntity dept = departmentMapper.selectById(req.getDepartmentId());
        if (dept == null || !"ENABLED".equals(dept.getStatus())) {
            throw new BusinessException("绑定的科室不存在或未启用");
        }

        // 2. 防重校验 (手机号)
        if (req.getPhone() != null) {
            Long count = doctorMapper.selectCount(new QueryWrapper<DoctorEntity>().eq("phone", req.getPhone()));
            if (count > 0) {
                throw new BusinessException("该手机号已绑定其他医生");
            }
        }

        // 3. 构建实体并保存
        String doctorNo = "DOC-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        DoctorEntity entity = new DoctorEntity();
        entity.setDoctorNo(doctorNo);
        entity.setName(req.getName());
        entity.setGender(req.getGender());
        entity.setTitle(req.getTitle());
        entity.setDepartmentId(req.getDepartmentId());
        entity.setIntroduction(req.getIntroduction());
        entity.setSpecialty(req.getSpecialty());
        entity.setPhone(req.getPhone());
        entity.setStatus("ENABLED");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        
        doctorMapper.insert(entity);

        // 4. 联动创建 SysUser 账号
        SysUserEntity user = new SysUserEntity();
        user.setUsername(req.getPhone()); // 默认手机号登录
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setUserType("DOCTOR");
        user.setBizId(entity.getId());
        user.setRealName(entity.getName());
        user.setPhone(entity.getPhone());
        user.setStatus("ENABLED");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeleted(false);
        sysUserMapper.insert(user);
        sysUserRoleService.bindSingleRoleIfAbsent(user.getId(), "DOCTOR", "医生");

        return convertToVO(entity, dept.getDeptName());
    }

    @Override
    public DoctorVO getDoctorById(Long id) {
        DoctorEntity entity = doctorMapper.selectById(id);
        if (entity == null) throw new BusinessException("医生不存在");
        DepartmentEntity dept = departmentMapper.selectById(entity.getDepartmentId());
        return convertToVO(entity, dept != null ? dept.getDeptName() : "");
    }

    @Override
    @Transactional
    public DoctorVO updateDoctor(Long id, DoctorUpdateReq req) {
        DoctorEntity entity = doctorMapper.selectById(id);
        if (entity == null) throw new BusinessException("医生不存在");

        if (req.getDepartmentId() != null) {
            DepartmentEntity dept = departmentMapper.selectById(req.getDepartmentId());
            if (dept == null || !"ENABLED".equals(dept.getStatus())) {
                throw new BusinessException("绑定的科室不存在或未启用");
            }
            entity.setDepartmentId(req.getDepartmentId());
        }

        if (StringUtils.hasText(req.getPhone())) {
            Long count = doctorMapper.selectCount(new QueryWrapper<DoctorEntity>().eq("phone", req.getPhone()).ne("id", id));
            if (count > 0) throw new BusinessException("该手机号已绑定其他医生");
            entity.setPhone(req.getPhone());
        }

        if (StringUtils.hasText(req.getName())) entity.setName(req.getName());
        if (StringUtils.hasText(req.getTitle())) entity.setTitle(req.getTitle());
        if (req.getIntroduction() != null) entity.setIntroduction(req.getIntroduction());
        if (req.getSpecialty() != null) entity.setSpecialty(req.getSpecialty());
        if (StringUtils.hasText(req.getStatus())) entity.setStatus(req.getStatus());

        entity.setUpdatedAt(LocalDateTime.now());
        doctorMapper.updateById(entity);

        DepartmentEntity dept = departmentMapper.selectById(entity.getDepartmentId());
        return convertToVO(entity, dept != null ? dept.getDeptName() : "");
    }

    @Override
    public Page<DoctorVO> getDoctorsPage(Integer pageNo, Integer pageSize, Long departmentId, String keyword) {
        Page<DoctorEntity> page = new Page<>(pageNo != null ? pageNo : 1, pageSize != null ? pageSize : 10);
        QueryWrapper<DoctorEntity> wrapper = new QueryWrapper<>();
        if (departmentId != null) {
            wrapper.eq("department_id", departmentId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like("name", keyword).or().like("phone", keyword);
        }
        wrapper.orderByDesc("created_at");
        doctorMapper.selectPage(page, wrapper);

        List<DoctorVO> voList = page.getRecords().stream().map(d -> {
            DepartmentEntity dept = departmentMapper.selectById(d.getDepartmentId());
            return convertToVO(d, dept != null ? dept.getDeptName() : "");
        }).collect(Collectors.toList());

        Page<DoctorVO> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    public List<DoctorVO> getDoctorsByDepartment(Long departmentId) {
        QueryWrapper<DoctorEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "ENABLED");
        if (departmentId != null) {
            wrapper.eq("department_id", departmentId);
        }
        List<DoctorEntity> list = doctorMapper.selectList(wrapper);
        return list.stream().map(d -> {
            DepartmentEntity dept = departmentMapper.selectById(d.getDepartmentId());
            String deptName = dept != null ? dept.getDeptName() : "";
            return convertToVO(d, deptName);
        }).collect(Collectors.toList());
    }

    private DoctorVO convertToVO(DoctorEntity entity, String deptName) {
        DoctorVO vo = new DoctorVO();
        vo.setId(entity.getId());
        vo.setDoctorNo(entity.getDoctorNo());
        vo.setName(entity.getName());
        vo.setGender(entity.getGender());
        vo.setTitle(entity.getTitle());
        vo.setDepartmentId(entity.getDepartmentId());
        vo.setDepartmentName(deptName);
        vo.setIntroduction(entity.getIntroduction());
        vo.setSpecialty(entity.getSpecialty());
        vo.setPhone(entity.getPhone());
        vo.setStatus(entity.getStatus());
        return vo;
    }
}
