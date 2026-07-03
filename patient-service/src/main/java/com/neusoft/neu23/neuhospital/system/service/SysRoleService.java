package com.neusoft.neu23.neuhospital.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.system.dto.RoleCreateReq;
import com.neusoft.neu23.neuhospital.system.dto.RoleUpdateReq;
import com.neusoft.neu23.neuhospital.system.vo.RoleVO;

public interface SysRoleService {

    RoleVO createRole(RoleCreateReq req);

    RoleVO updateRole(Long id, RoleUpdateReq req);

    RoleVO getRoleById(Long id);

    Page<RoleVO> getRolesPage(Integer pageNo, Integer pageSize, String keyword, String status);

    RoleVO ensureRole(String roleCode, String roleName);
}
