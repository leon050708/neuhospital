package com.neusoft.neu23.neuhospital.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.system.entity.SysRoleEntity;
import com.neusoft.neu23.neuhospital.system.entity.SysUserEntity;
import com.neusoft.neu23.neuhospital.system.entity.SysUserRoleEntity;
import com.neusoft.neu23.neuhospital.system.mapper.SysRoleMapper;
import com.neusoft.neu23.neuhospital.system.mapper.SysUserMapper;
import com.neusoft.neu23.neuhospital.system.mapper.SysUserRoleMapper;
import com.neusoft.neu23.neuhospital.system.service.SysRoleService;
import com.neusoft.neu23.neuhospital.system.service.SysUserRoleService;
import com.neusoft.neu23.neuhospital.system.vo.RoleVO;
import com.neusoft.neu23.neuhospital.system.vo.UserRoleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SysUserRoleServiceImpl implements SysUserRoleService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleService sysRoleService;

    public SysUserRoleServiceImpl(SysUserMapper sysUserMapper,
                                  SysRoleMapper sysRoleMapper,
                                  SysUserRoleMapper sysUserRoleMapper,
                                  SysRoleService sysRoleService) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRoleService = sysRoleService;
    }

    @Override
    public List<UserRoleVO> getUserRoles(Long userId) {
        requireUser(userId);
        List<SysUserRoleEntity> relations = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRoleEntity>()
                .eq(SysUserRoleEntity::getUserId, userId)
                .orderByAsc(SysUserRoleEntity::getId));
        if (relations.isEmpty()) {
            return List.of();
        }
        Map<Long, SysRoleEntity> roleMap = loadRoleMap(relations);
        List<UserRoleVO> result = new ArrayList<>();
        for (SysUserRoleEntity relation : relations) {
            SysRoleEntity role = roleMap.get(relation.getRoleId());
            if (role == null || Boolean.TRUE.equals(role.getDeleted())) {
                continue;
            }
            result.add(toVO(relation, role));
        }
        return result;
    }

    @Override
    @Transactional
    public List<UserRoleVO> assignRoles(Long userId, List<Long> roleIds) {
        requireUser(userId);
        Set<Long> normalizedRoleIds = roleIds == null ? Set.of() : roleIds.stream()
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (normalizedRoleIds.isEmpty()) {
            throw new BusinessException(400, "至少需要分配一个角色");
        }

        List<SysRoleEntity> roles = sysRoleMapper.selectBatchIds(normalizedRoleIds);
        if (roles.size() != normalizedRoleIds.size()) {
            throw new BusinessException(400, "存在无效角色");
        }
        boolean hasInvalidRole = roles.stream().anyMatch(role -> Boolean.TRUE.equals(role.getDeleted()));
        if (hasInvalidRole) {
            throw new BusinessException(400, "存在已删除角色，无法分配");
        }

        List<SysUserRoleEntity> existing = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRoleEntity>()
                .eq(SysUserRoleEntity::getUserId, userId));
        for (SysUserRoleEntity relation : existing) {
            sysUserRoleMapper.deleteById(relation.getId());
        }

        LocalDateTime now = LocalDateTime.now();
        for (Long roleId : normalizedRoleIds) {
            SysUserRoleEntity relation = new SysUserRoleEntity();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            relation.setCreatedAt(now);
            sysUserRoleMapper.insert(relation);
        }
        return getUserRoles(userId);
    }

    @Override
    @Transactional
    public void bindSingleRoleIfAbsent(Long userId, String roleCode, String roleName) {
        requireUser(userId);
        RoleVO role = sysRoleService.ensureRole(roleCode, roleName);
        Long count = sysUserRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRoleEntity>()
                .eq(SysUserRoleEntity::getUserId, userId)
                .eq(SysUserRoleEntity::getRoleId, role.getId()));
        if (count != null && count > 0) {
            return;
        }
        SysUserRoleEntity relation = new SysUserRoleEntity();
        relation.setUserId(userId);
        relation.setRoleId(role.getId());
        relation.setCreatedAt(LocalDateTime.now());
        sysUserRoleMapper.insert(relation);
    }

    private SysUserEntity requireUser(Long userId) {
        SysUserEntity user = sysUserMapper.selectById(userId);
        if (user == null || Boolean.TRUE.equals(user.getDeleted())) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private Map<Long, SysRoleEntity> loadRoleMap(List<SysUserRoleEntity> relations) {
        List<Long> roleIds = relations.stream().map(SysUserRoleEntity::getRoleId).toList();
        return sysRoleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(SysRoleEntity::getId, role -> role, (a, b) -> a, LinkedHashMap::new));
    }

    private UserRoleVO toVO(SysUserRoleEntity relation, SysRoleEntity role) {
        UserRoleVO vo = new UserRoleVO();
        vo.setRelationId(relation.getId());
        vo.setRoleId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setRoleStatus(role.getStatus());
        vo.setAssignedAt(relation.getCreatedAt());
        return vo;
    }
}
