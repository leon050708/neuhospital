package com.neusoft.neu23.neuhospital.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.neu23.neuhospital.doctor.entity.DepartmentEntity;
import com.neusoft.neu23.neuhospital.doctor.entity.DoctorEntity;
import com.neusoft.neu23.neuhospital.doctor.mapper.DepartmentMapper;
import com.neusoft.neu23.neuhospital.doctor.mapper.DoctorMapper;
import com.neusoft.neu23.neuhospital.system.entity.SysRoleEntity;
import com.neusoft.neu23.neuhospital.system.entity.SysUserEntity;
import com.neusoft.neu23.neuhospital.system.entity.SysUserRoleEntity;
import com.neusoft.neu23.neuhospital.system.mapper.SysRoleMapper;
import com.neusoft.neu23.neuhospital.system.mapper.SysUserMapper;
import com.neusoft.neu23.neuhospital.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    private final List<Long> userRoleIds = new ArrayList<>();
    private final List<Long> roleIds = new ArrayList<>();
    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> doctorIds = new ArrayList<>();
    private final List<Long> departmentIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        userRoleIds.forEach(sysUserRoleMapper::deleteById);
        userIds.forEach(sysUserMapper::deleteById);
        roleIds.forEach(sysRoleMapper::deleteById);
        doctorIds.forEach(doctorMapper::deleteById);
        departmentIds.forEach(departmentMapper::deleteById);
        userRoleIds.clear();
        roleIds.clear();
        userIds.clear();
        doctorIds.clear();
        departmentIds.clear();
    }

    @Test
    void loginShouldReadUserAndRoleFromDatabase() throws Exception {
        Long departmentId = createDepartment("CARD-" + System.nanoTime(), "心内科");
        Long doctorId = createDoctor("DOC-" + System.nanoTime(), "doctor-db-" + System.nanoTime(), departmentId);
        Long roleId = createRole("DOCTOR", "医生");
        String username = "doctor_db_" + System.nanoTime();
        createUserWithRole(username, "李医生", "DOCTOR", doctorId, roleId);

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", "password123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.userType").value("DOCTOR"))
                .andExpect(jsonPath("$.role").value("DOCTOR"))
                .andExpect(jsonPath("$.bizId").value(doctorId))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        String accessToken = objectMapper.readTree(loginResponse).get("accessToken").asText();
        assertThat(accessToken).isNotBlank();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }

    private Long createDepartment(String deptCode, String deptName) {
        DepartmentEntity entity = new DepartmentEntity();
        entity.setDeptCode(deptCode);
        entity.setDeptName(deptName);
        entity.setDeptType("OUTPATIENT");
        entity.setStatus("ENABLED");
        entity.setDeleted(false);
        departmentMapper.insert(entity);
        departmentIds.add(entity.getId());
        return entity.getId();
    }

    private Long createDoctor(String doctorNo, String name, Long departmentId) {
        DoctorEntity entity = new DoctorEntity();
        entity.setDoctorNo(doctorNo);
        entity.setName(name);
        entity.setGender("MALE");
        entity.setTitle("ATTENDING");
        entity.setDepartmentId(departmentId);
        entity.setStatus("ENABLED");
        entity.setDeleted(false);
        doctorMapper.insert(entity);
        doctorIds.add(entity.getId());
        return entity.getId();
    }

    private Long createRole(String roleCode, String roleName) {
        SysRoleEntity entity = new SysRoleEntity();
        entity.setRoleCode(roleCode);
        entity.setRoleName(roleName);
        entity.setStatus("ENABLED");
        entity.setDeleted(false);
        sysRoleMapper.insert(entity);
        roleIds.add(entity.getId());
        return entity.getId();
    }

    private void createUserWithRole(String username, String realName, String userType, Long bizId, Long roleId) {
        SysUserEntity user = new SysUserEntity();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setUserType(userType);
        user.setBizId(bizId);
        user.setRealName(realName);
        user.setPhone("13" + String.valueOf(System.nanoTime()).substring(0, 9));
        user.setStatus("ENABLED");
        user.setDeleted(false);
        sysUserMapper.insert(user);
        userIds.add(user.getId());

        SysUserRoleEntity relation = new SysUserRoleEntity();
        relation.setUserId(user.getId());
        relation.setRoleId(roleId);
        sysUserRoleMapper.insert(relation);
        userRoleIds.add(relation.getId());
    }
}
