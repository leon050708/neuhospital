package com.neusoft.neu23.neuhospital.ai.controller;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/api/ai/test")
public class DashScopeTestController {

    @GetMapping("/chat")
    public String chatWithDashScope(@RequestParam(defaultValue = "你好，请自我介绍一下。") String prompt) {
        try {
            Generation gen = new Generation();
            Message systemMsg = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content("你是一个有用的 AI 助手。")
                    .build();
            Message userMsg = Message.builder()
                    .role(Role.USER.getValue())
                    .content(prompt)
                    .build();
            GenerationParam param = GenerationParam.builder()
                    // 这里使用的是百炼 qwen-plus 模型，你也可以换成 qwen-turbo 或 qwen-max
                    .model("qwen-plus")
                    .messages(Arrays.asList(systemMsg, userMsg))
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();
                    
            // 这里我们不需要手动配置 API Key，SDK 默认会自动去系统环境变量读取 DASHSCOPE_API_KEY
            GenerationResult result = gen.call(param);
            return result.getOutput().getChoices().get(0).getMessage().getContent();
        } catch (NoApiKeyException | InputRequiredException e) {
            e.printStackTrace();
            return "API Key 未找到或输入异常: " + e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            return "调用报错: " + e.getMessage();
        }
    }
}
