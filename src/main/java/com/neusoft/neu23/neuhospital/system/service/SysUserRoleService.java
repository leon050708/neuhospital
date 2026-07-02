package com.neusoft.neu23.neuhospital.system.service;

import com.neusoft.neu23.neuhospital.system.vo.UserRoleVO;

import java.util.List;

public interface SysUserRoleService {

    List<UserRoleVO> getUserRoles(Long userId);

    List<UserRoleVO> assignRoles(Long userId, List<Long> roleIds);

    void bindSingleRoleIfAbsent(Long userId, String roleCode, String roleName);
}
