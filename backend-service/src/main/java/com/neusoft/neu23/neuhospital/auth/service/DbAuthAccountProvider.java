package com.neusoft.neu23.neuhospital.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.neu23.neuhospital.auth.security.AuthAccount;
import com.neusoft.neu23.neuhospital.system.entity.SysRoleEntity;
import com.neusoft.neu23.neuhospital.system.entity.SysUserEntity;
import com.neusoft.neu23.neuhospital.system.entity.SysUserRoleEntity;
import com.neusoft.neu23.neuhospital.system.mapper.SysRoleMapper;
import com.neusoft.neu23.neuhospital.system.mapper.SysUserMapper;
import com.neusoft.neu23.neuhospital.system.mapper.SysUserRoleMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * DB-backed account provider reading sys_user and role relations.
 */
@Component
@Primary
public class DbAuthAccountProvider implements AuthAccountProvider {

    private static final String STATUS_ENABLED = "ENABLED";

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;

    public DbAuthAccountProvider(SysUserMapper sysUserMapper,
                                 SysUserRoleMapper sysUserRoleMapper,
                                 SysRoleMapper sysRoleMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRoleMapper = sysRoleMapper;
    }

    @Override
    public AuthAccount findByUsername(String username) {
        SysUserEntity user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getUsername, username)
                .eq(SysUserEntity::getDeleted, false)
                .last("limit 1"));
        if (user == null) {
            return null;
        }

        String roleCode = resolvePrimaryRoleCode(user.getId(), user.getUserType());
        return new AuthAccount(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getUserType(),
                roleCode,
                user.getBizId(),
                user.getStatus()
        );
    }

    /**
     * The current auth chain only carries a single role string in JWT.
     * For now we pick the first enabled role relation by insertion order.
     */
    private String resolvePrimaryRoleCode(Long userId, String fallbackRole) {
        List<SysUserRoleEntity> userRoles = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRoleEntity>()
                .eq(SysUserRoleEntity::getUserId, userId)
                .orderByAsc(SysUserRoleEntity::getId));
        if (userRoles.isEmpty()) {
            return fallbackRole;
        }

        List<Long> roleIds = userRoles.stream()
                .map(SysUserRoleEntity::getRoleId)
                .toList();
        Map<Long, SysRoleEntity> roleMap = sysRoleMapper.selectBatchIds(roleIds).stream()
                .filter(role -> Boolean.FALSE.equals(role.getDeleted()) && STATUS_ENABLED.equals(role.getStatus()))
                .collect(Collectors.toMap(SysRoleEntity::getId, Function.identity()));

        for (SysUserRoleEntity userRole : userRoles) {
            SysRoleEntity role = roleMap.get(userRole.getRoleId());
            if (role != null) {
                return role.getRoleCode();
            }
        }
        return fallbackRole;
    }
}
