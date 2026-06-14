package com.neusoft.neu23.neuhospital.doctor.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.neu23.neuhospital.doctor.dto.DepartmentCreateReq;
import com.neusoft.neu23.neuhospital.doctor.dto.DoctorCreateReq;
import com.neusoft.neu23.neuhospital.doctor.entity.DepartmentEntity;
import com.neusoft.neu23.neuhospital.doctor.mapper.DepartmentMapper;
import com.neusoft.neu23.neuhospital.system.entity.SysUserEntity;
import com.neusoft.neu23.neuhospital.system.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class DoctorControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Test
    public void testCreateDoctor_InvalidDepartment_ThrowsException() throws Exception {
        DoctorCreateReq docReq = new DoctorCreateReq();
        docReq.setName("Test Doctor");
        docReq.setDepartmentId(9999L); // Invalid Dept ID
        docReq.setPhone("13900139000");

        mockMvc.perform(post("/api/doctors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(docReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("绑定的科室不存在或未启用"));
    }

    @Test
    public void testCreateDoctor_Success_And_AuthAccountCreated() throws Exception {
        // 先手动插一个科室
        DepartmentEntity dept = new DepartmentEntity();
        dept.setDeptCode("DEPT-DOC-TEST");
        dept.setDeptName("测试科室");
        dept.setStatus("ENABLED");
        departmentMapper.insert(dept);

        DoctorCreateReq docReq = new DoctorCreateReq();
        docReq.setName("张医生");
        docReq.setDepartmentId(dept.getId());
        docReq.setPhone("13900139001");
        docReq.setGender("MALE");
        docReq.setTitle("主任医师");

        mockMvc.perform(post("/api/doctors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(docReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.doctorNo").exists())
                .andExpect(jsonPath("$.data.departmentName").value("测试科室"));

        // Verify that sys_user is created
        SysUserEntity user = sysUserMapper.selectOne(new QueryWrapper<SysUserEntity>()
                .eq("phone", "13900139001")
                .eq("user_type", "DOCTOR"));
        assertNotNull(user, "sys_user should be created for the new doctor");
    }
}
