package com.neusoft.neu23.neuhospital.ai.application.tool;

import com.neusoft.neu23.neuhospital.patient.dto.PatientUpdateReq;
import com.neusoft.neu23.neuhospital.patient.service.PatientService;
import com.neusoft.neu23.neuhospital.patient.vo.PatientVO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration
public class UpdatePatientInfoTool {

    private final PatientService patientService;

    public UpdatePatientInfoTool(PatientService patientService) {
        this.patientService = patientService;
    }

    public record Request(Long patientId, String newAllergy, String newHistory) {}

    @Bean
    @Description("动态结构化记忆更新工具。当你在与患者聊天时，如果患者明确陈述了新的【过敏原】（如芒果过敏、青霉素过敏）或新的【长期慢病/既往史】（如高血压、糖尿病），调用此工具将其永久写入患者的医疗档案中。")
    public Function<Request, String> updatePatientMemory() {
        return request -> {
            try {
                // 先查询已有数据
                PatientVO patient = patientService.getPatientById(request.patientId());
                if (patient == null) {
                    return "更新失败：未找到ID为 " + request.patientId() + " 的患者记录。";
                }

                PatientUpdateReq updateReq = new PatientUpdateReq();
                updateReq.setName(patient.getName()); // 必须带上，因为部分校验可能需要

                boolean updated = false;

                // 追加过敏史
                if (request.newAllergy() != null && !request.newAllergy().trim().isEmpty()) {
                    String current = patient.getAllergySummary() == null ? "" : patient.getAllergySummary();
                    if (!current.contains(request.newAllergy())) {
                        updateReq.setAllergySummary(current.isEmpty() ? request.newAllergy() : current + "；" + request.newAllergy());
                        updated = true;
                    }
                } else {
                    updateReq.setAllergySummary(patient.getAllergySummary());
                }

                // 追加既往史
                if (request.newHistory() != null && !request.newHistory().trim().isEmpty()) {
                    String current = patient.getHistorySummary() == null ? "" : patient.getHistorySummary();
                    if (!current.contains(request.newHistory())) {
                        updateReq.setHistorySummary(current.isEmpty() ? request.newHistory() : current + "；" + request.newHistory());
                        updated = true;
                    }
                } else {
                    updateReq.setHistorySummary(patient.getHistorySummary());
                }

                if (!updated) {
                    return "提取的信息在患者档案中已存在，无需重复更新。";
                }

                // 其余字段透传以防丢失
                updateReq.setPhone(patient.getPhone());
                updateReq.setBloodType(patient.getBloodType());
                updateReq.setStatus(patient.getStatus());

                patientService.updatePatient(request.patientId(), updateReq);

                return "患者长期健康档案更新成功！新的过敏史和既往史已成功录入系统。";
            } catch (Exception e) {
                return "更新患者健康档案时出错：" + e.getMessage();
            }
        };
    }
}
