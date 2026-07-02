package com.neusoft.neu23.neuhospital.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.common.response.PageResult;
import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.system.dto.RoleCreateReq;
import com.neusoft.neu23.neuhospital.system.dto.RoleUpdateReq;
import com.neusoft.neu23.neuhospital.system.service.SysRoleService;
import com.neusoft.neu23.neuhospital.system.vo.RoleVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/roles")
@PreAuthorize("hasAnyRole('ADMIN','MANAGEMENT')")
public class SysRoleController {

    private final SysRoleService sysRoleService;

    public SysRoleController(SysRoleService sysRoleService) {
        this.sysRoleService = sysRoleService;
    }

    @PostMapping
    public Result<RoleVO> createRole(@RequestBody RoleCreateReq req) {
        return Result.success(sysRoleService.createRole(req));
    }

    @PutMapping("/{id}")
    public Result<RoleVO> updateRole(@PathVariable Long id, @RequestBody RoleUpdateReq req) {
        return Result.success(sysRoleService.updateRole(id, req));
    }

    @GetMapping("/{id}")
    public Result<RoleVO> getRole(@PathVariable Long id) {
        return Result.success(sysRoleService.getRoleById(id));
    }

    @GetMapping
    public Result<PageResult<RoleVO>> getRolesPage(
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status) {
        Page<RoleVO> page = sysRoleService.getRolesPage(pageNo, pageSize, keyword, status);
        return Result.success(new PageResult<>(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal()));
    }
}
