package com.neusoft.neu23.neuhospital.ai.application.tool;

import com.neusoft.neu23.neuhospital.ai.application.agent.AiAgentSessionContext;
import com.neusoft.neu23.neuhospital.patient.service.PatientService;
import com.neusoft.neu23.neuhospital.patient.vo.PatientVO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration
public class PatientInfoTool {

    private final PatientService patientService;

    public PatientInfoTool(PatientService patientService) {
        this.patientService = patientService;
    }

    public record Request() {}

    @Bean
    @Description("获取当前就诊患者的基本信息、过敏史和既往史。在每次为新患者问诊前必须首先调用此工具了解患者身体状况。")
    public Function<Request, String> getPatientInfo() {
        return request -> {
            try {
                Long patientId = AiAgentSessionContext.requirePatientId();
                PatientVO patient = patientService.getPatientById(patientId);
                if (patient == null) {
                    return "未找到当前会话绑定的患者记录。";
                }
                return String.format("患者基本信息档案：\n姓名：%s\n性别：%s\n出生日期：%s\n血型：%s\n【过敏史】：%s\n【既往史】：%s",
                        patient.getName(),
                        patient.getGender(),
                        patient.getBirthDate() != null ? patient.getBirthDate().toString() : "未知",
                        patient.getBloodType() != null ? patient.getBloodType() : "未知",
                        patient.getAllergySummary() != null && !patient.getAllergySummary().isEmpty() ? patient.getAllergySummary() : "无明确过敏史",
                        patient.getHistorySummary() != null && !patient.getHistorySummary().isEmpty() ? patient.getHistorySummary() : "无明确既往史"
                );
            } catch (Exception e) {
                return "获取患者信息出错：" + e.getMessage();
            }
        };
    }
}
