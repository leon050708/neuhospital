package com.neusoft.neu23.neuhospital.ai.application.tool;

import com.neusoft.neu23.neuhospital.ai.application.agent.AiAgentSessionContext;
import com.neusoft.neu23.neuhospital.registration.dto.RegistrationCreateReq;
import com.neusoft.neu23.neuhospital.registration.service.RegistrationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration
public class BookRegistrationTool {

    private final RegistrationService registrationService;

    public BookRegistrationTool(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    public record Request(Long scheduleId) {}

    @Bean
    @Description("患者确认要挂号时调用此工具。只允许提供 scheduleId，患者身份必须以当前AI会话绑定的患者为准。调用成功即代表挂号成功。")
    public Function<Request, String> bookRegistration() {
        return request -> {
            try {
                if (request.scheduleId() == null) {
                    return "挂号失败：scheduleId 不能为空";
                }
                Long patientId = AiAgentSessionContext.requirePatientId();
                RegistrationCreateReq req = new RegistrationCreateReq();
                req.setPatientId(patientId);
                req.setScheduleId(request.scheduleId());
                String msgId = registrationService.quickRegister(req);
                return "挂号受理成功！单号/消息ID：" + msgId;
            } catch (Exception e) {
                return "挂号失败：" + e.getMessage();
            }
        };
    }
}
