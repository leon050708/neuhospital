package com.neusoft.neu23.neuhospital.doctor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.doctor.dto.DepartmentCreateReq;
import com.neusoft.neu23.neuhospital.doctor.dto.DepartmentUpdateReq;
import com.neusoft.neu23.neuhospital.doctor.entity.DepartmentEntity;
import com.neusoft.neu23.neuhospital.doctor.mapper.DepartmentMapper;
import com.neusoft.neu23.neuhospital.doctor.service.DepartmentService;
import com.neusoft.neu23.neuhospital.doctor.vo.DepartmentVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;

    public DepartmentServiceImpl(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    @Override
    public DepartmentVO createDepartment(DepartmentCreateReq req) {
        // 防重校验
        Long count = departmentMapper.selectCount(new QueryWrapper<DepartmentEntity>().eq("dept_code", req.getDeptCode()));
        if (count > 0) {
            throw new BusinessException("该科室编码已存在，无法重复创建");
        }

        DepartmentEntity entity = new DepartmentEntity();
        entity.setDeptCode(req.getDeptCode());
        entity.setDeptName(req.getDeptName());
        entity.setDeptType(req.getDeptType());
        entity.setDescription(req.getDescription());
        entity.setStatus("ENABLED");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        
        departmentMapper.insert(entity);

        return convertToVO(entity);
    }

    @Override
    public DepartmentVO getDepartmentById(Long id) {
        DepartmentEntity entity = departmentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("科室不存在");
        }
        return convertToVO(entity);
    }

    @Override
    public DepartmentVO updateDepartment(Long id, DepartmentUpdateReq req) {
        DepartmentEntity entity = departmentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("科室不存在");
        }
        if (StringUtils.hasText(req.getDeptName())) entity.setDeptName(req.getDeptName());
        if (StringUtils.hasText(req.getDeptType())) entity.setDeptType(req.getDeptType());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        if (StringUtils.hasText(req.getStatus())) entity.setStatus(req.getStatus());

        entity.setUpdatedAt(LocalDateTime.now());
        departmentMapper.updateById(entity);
        return convertToVO(entity);
    }

    @Override
    public List<DepartmentVO> getAllDepartments() {
        List<DepartmentEntity> list = departmentMapper.selectList(new QueryWrapper<DepartmentEntity>().eq("status", "ENABLED"));
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    private DepartmentVO convertToVO(DepartmentEntity entity) {
        DepartmentVO vo = new DepartmentVO();
        vo.setId(entity.getId());
        vo.setDeptCode(entity.getDeptCode());
        vo.setDeptName(entity.getDeptName());
        vo.setDeptType(entity.getDeptType());
        vo.setDescription(entity.getDescription());
        vo.setStatus(entity.getStatus());
        return vo;
    }
}
