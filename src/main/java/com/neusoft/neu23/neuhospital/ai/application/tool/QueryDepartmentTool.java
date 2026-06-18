package com.neusoft.neu23.neuhospital.ai.application.tool;

import com.neusoft.neu23.neuhospital.doctor.service.DepartmentService;
import com.neusoft.neu23.neuhospital.doctor.vo.DepartmentVO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class QueryDepartmentTool {

    private final DepartmentService departmentService;

    public QueryDepartmentTool(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    public record Request() {}

    @Bean
    @Description("获取医院所有科室列表。当需要向患者推荐科室时调用此工具以获取准确的科室ID和名称。")
    public Function<Request, String> queryDepartment() {
        return request -> {
            List<DepartmentVO> list = departmentService.getAllDepartments();
            if (list == null || list.isEmpty()) {
                return "未查到任何科室";
            }
            return list.stream()
                    .map(d -> String.format("科室ID: %d, 名称: %s", d.getId(), d.getDeptName()))
                    .collect(Collectors.joining("\n"));
        };
    }
}
