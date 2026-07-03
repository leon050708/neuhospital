package com.neusoft.neu23.neuhospital.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.system.dto.RoleCreateReq;
import com.neusoft.neu23.neuhospital.system.dto.RoleUpdateReq;
import com.neusoft.neu23.neuhospital.system.entity.SysRoleEntity;
import com.neusoft.neu23.neuhospital.system.mapper.SysRoleMapper;
import com.neusoft.neu23.neuhospital.system.service.SysRoleService;
import com.neusoft.neu23.neuhospital.system.vo.RoleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class SysRoleServiceImpl implements SysRoleService {

    private static final String STATUS_ENABLED = "ENABLED";

    private final SysRoleMapper sysRoleMapper;

    public SysRoleServiceImpl(SysRoleMapper sysRoleMapper) {
        this.sysRoleMapper = sysRoleMapper;
    }

    @Override
    @Transactional
    public RoleVO createRole(RoleCreateReq req) {
        validateCreateRequest(req);
        ensureRoleCodeNotExists(req.getRoleCode(), null);

        SysRoleEntity entity = new SysRoleEntity();
        entity.setRoleCode(req.getRoleCode().trim().toUpperCase());
        entity.setRoleName(req.getRoleName().trim());
        entity.setDescription(req.getDescription());
        entity.setStatus(StringUtils.hasText(req.getStatus()) ? req.getStatus().trim().toUpperCase() : STATUS_ENABLED);
        entity.setDeleted(false);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        sysRoleMapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public RoleVO updateRole(Long id, RoleUpdateReq req) {
        SysRoleEntity entity = requireRole(id);
        if (StringUtils.hasText(req.getRoleName())) {
            entity.setRoleName(req.getRoleName().trim());
        }
        if (req.getDescription() != null) {
            entity.setDescription(req.getDescription());
        }
        if (StringUtils.hasText(req.getStatus())) {
            entity.setStatus(req.getStatus().trim().toUpperCase());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        sysRoleMapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    public RoleVO getRoleById(Long id) {
        return toVO(requireRole(id));
    }

    @Override
    public Page<RoleVO> getRolesPage(Integer pageNo, Integer pageSize, String keyword, String status) {
        Page<SysRoleEntity> page = new Page<>(pageNo != null ? pageNo : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<SysRoleEntity> wrapper = new LambdaQueryWrapper<SysRoleEntity>()
                .eq(SysRoleEntity::getDeleted, false);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysRoleEntity::getRoleCode, keyword)
                    .or()
                    .like(SysRoleEntity::getRoleName, keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysRoleEntity::getStatus, status.trim().toUpperCase());
        }
        wrapper.orderByDesc(SysRoleEntity::getCreatedAt);
        sysRoleMapper.selectPage(page, wrapper);

        Page<RoleVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return result;
    }

    @Override
    @Transactional
    public RoleVO ensureRole(String roleCode, String roleName) {
        if (!StringUtils.hasText(roleCode)) {
            throw new BusinessException(400, "角色编码不能为空");
        }
        String normalizedCode = roleCode.trim().toUpperCase();
        SysRoleEntity existing = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRoleEntity>()
                .eq(SysRoleEntity::getRoleCode, normalizedCode)
                .eq(SysRoleEntity::getDeleted, false)
                .last("limit 1"));
        if (existing != null) {
            if (!STATUS_ENABLED.equals(existing.getStatus())) {
                existing.setStatus(STATUS_ENABLED);
                existing.setUpdatedAt(LocalDateTime.now());
                sysRoleMapper.updateById(existing);
            }
            return toVO(existing);
        }

        RoleCreateReq req = new RoleCreateReq();
        req.setRoleCode(normalizedCode);
        req.setRoleName(StringUtils.hasText(roleName) ? roleName.trim() : normalizedCode);
        req.setStatus(STATUS_ENABLED);
        return createRole(req);
    }

    private void validateCreateRequest(RoleCreateReq req) {
        if (req == null) {
            throw new BusinessException(400, "角色参数不能为空");
        }
        if (!StringUtils.hasText(req.getRoleCode())) {
            throw new BusinessException(400, "角色编码不能为空");
        }
        if (!StringUtils.hasText(req.getRoleName())) {
            throw new BusinessException(400, "角色名称不能为空");
        }
    }

    private void ensureRoleCodeNotExists(String roleCode, Long excludeId) {
        LambdaQueryWrapper<SysRoleEntity> wrapper = new LambdaQueryWrapper<SysRoleEntity>()
                .eq(SysRoleEntity::getRoleCode, roleCode.trim().toUpperCase())
                .eq(SysRoleEntity::getDeleted, false);
        if (excludeId != null) {
            wrapper.ne(SysRoleEntity::getId, excludeId);
        }
        Long count = sysRoleMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(400, "角色编码已存在");
        }
    }

    private SysRoleEntity requireRole(Long id) {
        SysRoleEntity entity = sysRoleMapper.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getDeleted())) {
            throw new BusinessException(404, "角色不存在");
        }
        return entity;
    }

    private RoleVO toVO(SysRoleEntity entity) {
        RoleVO vo = new RoleVO();
        vo.setId(entity.getId());
        vo.setRoleCode(entity.getRoleCode());
        vo.setRoleName(entity.getRoleName());
        vo.setDescription(entity.getDescription());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
