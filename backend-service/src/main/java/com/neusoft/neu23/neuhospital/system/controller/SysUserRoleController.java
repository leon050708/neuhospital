package com.neusoft.neu23.neuhospital.system.controller;

import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.system.dto.UserRoleAssignReq;
import com.neusoft.neu23.neuhospital.system.service.SysUserRoleService;
import com.neusoft.neu23.neuhospital.system.vo.UserRoleVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/users")
@PreAuthorize("hasAnyRole('ADMIN','MANAGEMENT')")
public class SysUserRoleController {

    private final SysUserRoleService sysUserRoleService;

    public SysUserRoleController(SysUserRoleService sysUserRoleService) {
        this.sysUserRoleService = sysUserRoleService;
    }

    @GetMapping("/{userId}/roles")
    public Result<List<UserRoleVO>> getUserRoles(@PathVariable Long userId) {
        return Result.success(sysUserRoleService.getUserRoles(userId));
    }

    @PutMapping("/{userId}/roles")
    public Result<List<UserRoleVO>> assignRoles(@PathVariable Long userId, @RequestBody UserRoleAssignReq req) {
        return Result.success(sysUserRoleService.assignRoles(userId, req == null ? null : req.getRoleIds()));
    }
}
