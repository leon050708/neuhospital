package com.neusoft.neu23.neuhospital.patient.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.neu23.neuhospital.patient.dto.PatientCreateReq;
import com.neusoft.neu23.neuhospital.system.entity.SysUserEntity;
import com.neusoft.neu23.neuhospital.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "test-admin", roles = "ADMIN")
public class PatientControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Test
    public void testCreatePatient_Success_And_AuthAccountCreated() throws Exception {
        long suffix = System.nanoTime();
        String phone = "139" + String.valueOf(suffix).substring(0, 8);
        String idCard = "11010519900101" + String.valueOf(suffix).substring(0, 4);
        PatientCreateReq req = new PatientCreateReq();
        req.setName("Test Patient");
        req.setIdCard(idCard);
        req.setPhone(phone);
        req.setGender("MALE");
        req.setBirthDate(LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/api/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.patientNo").exists());

        // Verify that sys_user is created
        SysUserEntity user = sysUserMapper.selectOne(new QueryWrapper<SysUserEntity>()
                .eq("phone", phone)
                .eq("user_type", "PATIENT"));
        assertNotNull(user, "sys_user should be created for the new patient");
    }

    @Test
    public void testCreatePatient_DuplicateIdCard_ThrowsException() throws Exception {
        long suffix = System.nanoTime();
        String duplicateIdCard = "11010519900202" + String.valueOf(suffix).substring(0, 4);
        PatientCreateReq req1 = new PatientCreateReq();
        req1.setName("Test Patient 1");
        req1.setIdCard(duplicateIdCard);
        req1.setPhone("137" + String.valueOf(suffix).substring(0, 8));
        req1.setGender("MALE");
        
        mockMvc.perform(post("/api/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        PatientCreateReq req2 = new PatientCreateReq();
        req2.setName("Test Patient 2");
        req2.setIdCard(duplicateIdCard); // duplicate ID card
        req2.setPhone("136" + String.valueOf(suffix).substring(0, 8));
        req2.setGender("FEMALE");

        mockMvc.perform(post("/api/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("该身份证号已建档，无法重复创建"));
    }
}
