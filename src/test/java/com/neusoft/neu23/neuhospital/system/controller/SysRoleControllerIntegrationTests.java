package com.neusoft.neu23.neuhospital.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.neu23.neuhospital.system.dto.RoleCreateReq;
import com.neusoft.neu23.neuhospital.system.dto.UserRoleAssignReq;
import com.neusoft.neu23.neuhospital.system.entity.SysUserEntity;
import com.neusoft.neu23.neuhospital.system.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SysRoleControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Test
    @WithMockUser(username = "sys-admin", roles = "ADMIN")
    void adminShouldCreateRoleAndAssignUserRoles() throws Exception {
        RoleCreateReq roleCreateReq = new RoleCreateReq();
        roleCreateReq.setRoleCode("TEST_ADMIN_" + System.nanoTime());
        roleCreateReq.setRoleName("测试管理员");
        roleCreateReq.setStatus("ENABLED");

        String createResp = mockMvc.perform(post("/api/system/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleCreateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.roleCode").value(roleCreateReq.getRoleCode()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long roleId = objectMapper.readTree(createResp).path("data").path("id").asLong();

        SysUserEntity user = new SysUserEntity();
        user.setUsername("role_user_" + System.nanoTime());
        user.setPasswordHash("noop");
        user.setUserType("MANAGEMENT");
        user.setRealName("角色测试用户");
        user.setStatus("ENABLED");
        user.setDeleted(false);
        sysUserMapper.insert(user);

        UserRoleAssignReq assignReq = new UserRoleAssignReq();
        assignReq.setRoleIds(List.of(roleId));

        mockMvc.perform(put("/api/system/users/{userId}/roles", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].roleId").value(roleId));

        mockMvc.perform(get("/api/system/users/{userId}/roles", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].roleCode").value(roleCreateReq.getRoleCode()));
    }

    @Test
    @WithMockUser(username = "doctor-user", roles = "DOCTOR")
    void nonAdminShouldNotAccessRoleManagement() throws Exception {
        RoleCreateReq roleCreateReq = new RoleCreateReq();
        roleCreateReq.setRoleCode("FORBIDDEN_" + System.nanoTime());
        roleCreateReq.setRoleName("禁止访问");

        mockMvc.perform(post("/api/system/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleCreateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("System Error: Access Denied"));
    }
}
