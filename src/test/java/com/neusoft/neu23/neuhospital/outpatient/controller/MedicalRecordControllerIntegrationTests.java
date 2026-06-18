package com.neusoft.neu23.neuhospital.outpatient.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.neu23.neuhospital.outpatient.dto.MedicalDiagnosisReq;
import com.neusoft.neu23.neuhospital.outpatient.dto.MedicalRecordCreateReq;
import com.neusoft.neu23.neuhospital.outpatient.dto.MedicalRecordUpdateReq;
import com.neusoft.neu23.neuhospital.outpatient.service.MedicalRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // 保证每个测试后数据回滚
@WithMockUser(username = "test-doctor")
public class MedicalRecordControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    void testCreateAndUpdateRecordFlow() throws Exception {
        // 0. 准备前置外键数据
        jdbcTemplate.update("INSERT INTO patient (id, patient_no, name, id_card, gender, phone, status) VALUES (20001, 'PAT20001', '张三', '110105199001011234', 'MALE', '13800000000', 'ENABLED') ON CONFLICT DO NOTHING");
        jdbcTemplate.update("INSERT INTO department (id, dept_code, dept_name, dept_type, status) VALUES (40001, 'NEURO', '神经内科', 'CLINICAL', 'ENABLED') ON CONFLICT DO NOTHING");
        jdbcTemplate.update("INSERT INTO doctor (id, doctor_no, name, department_id, title, status) VALUES (30001, 'DOC30001', '李医生', 40001, 'CHIEF', 'ENABLED') ON CONFLICT DO NOTHING");
        jdbcTemplate.update("INSERT INTO doctor_schedule (id, doctor_id, department_id, schedule_date, time_slot, source_count, available_count, fee_amount, source_type, status) VALUES (60001, 30001, 40001, CURRENT_DATE, 'MORNING', 20, 20, 50.0, 'NORMAL', 'ENABLED') ON CONFLICT DO NOTHING");
        jdbcTemplate.update("INSERT INTO registration (id, registration_no, patient_id, doctor_id, department_id, schedule_id, visit_date, time_slot, status, fee_amount, registered_at) VALUES (50001, 'REG20231010', 20001, 30001, 40001, 60001, CURRENT_DATE, 'MORNING', 'COMPLETED', 50.0, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING");

        // 1. 新建草稿病历，同时传入诊断
        MedicalRecordCreateReq createReq = new MedicalRecordCreateReq();
        createReq.setRegistrationId(50001L);
        createReq.setPatientId(20001L);
        createReq.setDoctorId(30001L);
        createReq.setDepartmentId(40001L);
        createReq.setChiefComplaint("头痛3天，加重1天");

        MedicalDiagnosisReq diag1 = new MedicalDiagnosisReq();
        diag1.setDiseaseName("急性偏头痛");
        diag1.setDiagnosisType("MAIN");
        diag1.setSuspectedFlag(false);
        createReq.setDiagnoses(Arrays.asList(diag1));

        MvcResult result = mockMvc.perform(post("/api/outpatient/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Long recordId = objectMapper.readTree(content).get("data").asLong();
        assertNotNull(recordId);

        // 2. 查询病历详情，验证初始状态为 DRAFT 且能带出诊断
        mockMvc.perform(get("/api/outpatient/records/" + recordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.chiefComplaint").value("头痛3天，加重1天"))
                .andExpect(jsonPath("$.data.diagnoses[0].diseaseName").value("急性偏头痛"));

        // 3. 修改病历，补充现病史
        MedicalRecordUpdateReq updateReq = new MedicalRecordUpdateReq();
        updateReq.setChiefComplaint("头痛3天，加重1天");
        updateReq.setPresentIllness("无恶心呕吐");
        
        mockMvc.perform(put("/api/outpatient/records/" + recordId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 4. 确认病历
        mockMvc.perform(post("/api/outpatient/records/" + recordId + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 5. 再次查询，验证状态为 CONFIRMED
        mockMvc.perform(get("/api/outpatient/records/" + recordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.presentIllness").value("无恶心呕吐"));

        // 6. 尝试修改已确认的病历（预期报错）
        mockMvc.perform(put("/api/outpatient/records/" + recordId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk()) // Global exception handler 捕获后返回 500/400 JSON 结构
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("病历已确认，不可修改"));
    }
}
