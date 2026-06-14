package com.neusoft.neu23.neuhospital.registration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.neu23.neuhospital.doctor.entity.DepartmentEntity;
import com.neusoft.neu23.neuhospital.doctor.entity.DoctorEntity;
import com.neusoft.neu23.neuhospital.doctor.mapper.DepartmentMapper;
import com.neusoft.neu23.neuhospital.doctor.mapper.DoctorMapper;
import com.neusoft.neu23.neuhospital.registration.dto.DoctorScheduleCreateReq;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class DoctorScheduleControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Test
    public void testCreateSchedule_Success() throws Exception {
        // Mock a department and doctor
        DepartmentEntity dept = new DepartmentEntity();
        dept.setDeptCode("DEPT-SCHED");
        dept.setDeptName("排班测试科室");
        dept.setStatus("ENABLED");
        departmentMapper.insert(dept);

        DoctorEntity doc = new DoctorEntity();
        doc.setDoctorNo("DOC-SCHED");
        doc.setName("排班医生");
        doc.setDepartmentId(dept.getId());
        doc.setStatus("ENABLED");
        doctorMapper.insert(doc);

        DoctorScheduleCreateReq req = new DoctorScheduleCreateReq();
        req.setDoctorId(doc.getId());
        req.setDepartmentId(dept.getId());
        req.setScheduleDate(LocalDate.now().plusDays(1));
        req.setTimeSlot("AM");
        req.setSourceCount(20);
        req.setFeeAmount(new BigDecimal("50.00"));
        req.setSourceType("NORMAL");

        mockMvc.perform(post("/api/schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.availableCount").value(20));
    }

    @Test
    public void testCreateSchedule_Duplicate_ThrowsException() throws Exception {
        DepartmentEntity dept = new DepartmentEntity();
        dept.setDeptCode("DEPT-SCHED-DUP");
        dept.setDeptName("科室2");
        dept.setStatus("ENABLED");
        departmentMapper.insert(dept);

        DoctorEntity doc = new DoctorEntity();
        doc.setDoctorNo("DOC-SCHED-DUP");
        doc.setName("医生2");
        doc.setDepartmentId(dept.getId());
        doc.setStatus("ENABLED");
        doctorMapper.insert(doc);

        DoctorScheduleCreateReq req = new DoctorScheduleCreateReq();
        req.setDoctorId(doc.getId());
        req.setDepartmentId(dept.getId());
        req.setScheduleDate(LocalDate.now().plusDays(2));
        req.setTimeSlot("PM");
        req.setSourceCount(10);
        req.setFeeAmount(new BigDecimal("100.00"));
        req.setSourceType("EXPERT");

        // First time
        mockMvc.perform(post("/api/schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Second time (duplicate)
        mockMvc.perform(post("/api/schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("该医生在此时间段已有排班"));
    }
}
