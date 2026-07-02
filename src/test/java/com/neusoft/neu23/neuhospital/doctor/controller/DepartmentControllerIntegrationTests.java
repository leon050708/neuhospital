package com.neusoft.neu23.neuhospital.doctor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.neu23.neuhospital.doctor.dto.DepartmentCreateReq;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "test-admin", roles = "ADMIN")
public class DepartmentControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCreateDepartment_Success() throws Exception {
        DepartmentCreateReq req = new DepartmentCreateReq();
        req.setDeptCode("DEPT-001");
        req.setDeptName("内科");
        req.setDeptType("CLINICAL");
        req.setDescription("内科综合");

        mockMvc.perform(post("/api/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    public void testCreateDepartment_DuplicateCode_ThrowsException() throws Exception {
        DepartmentCreateReq req1 = new DepartmentCreateReq();
        req1.setDeptCode("DEPT-002");
        req1.setDeptName("外科");

        mockMvc.perform(post("/api/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        DepartmentCreateReq req2 = new DepartmentCreateReq();
        req2.setDeptCode("DEPT-002"); // duplicate code
        req2.setDeptName("神经外科");

        mockMvc.perform(post("/api/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("该科室编码已存在，无法重复创建"));
    }
}
