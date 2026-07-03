package com.neusoft.neu23.neuhospital.ct.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.ct.config.CtAnalysisProperties;
import com.neusoft.neu23.neuhospital.ct.vo.B1B2InferenceResult;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CtAiInferenceService {

    private final ObjectMapper objectMapper;
    private final CtAnalysisProperties ctAnalysisProperties;

    public CtAiInferenceService(ObjectMapper objectMapper, CtAnalysisProperties ctAnalysisProperties) {
        this.objectMapper = objectMapper;
        this.ctAnalysisProperties = ctAnalysisProperties;
    }

    public B1B2InferenceResult infer(String caseDir) {
        List<String> command = new ArrayList<>();
        command.add(ctAnalysisProperties.getPythonExecutable());
        command.add(ctAnalysisProperties.getScriptPath());
        command.add("--input");
        command.add(caseDir);

        if (ctAnalysisProperties.getModelPath() != null && !ctAnalysisProperties.getModelPath().isBlank()) {
            command.add("--model");
            command.add(ctAnalysisProperties.getModelPath());
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(Path.of("").toAbsolutePath().toFile());

        try {
            Process process = processBuilder.start();
            String output;
            String errorOutput;
            try (InputStream inputStream = process.getInputStream();
                 InputStream errorStream = process.getErrorStream()) {
                boolean finished = process.waitFor(ctAnalysisProperties.getTimeoutSeconds(), TimeUnit.SECONDS);
                output = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
                errorOutput = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8).trim();
                if (!finished) {
                    process.destroyForcibly();
                    throw new BusinessException("CT 推理超时，请稍后重试");
                }
            }

            if (process.exitValue() != 0) {
                String detail = output;
                if (detail == null || detail.isBlank()) {
                    detail = errorOutput;
                } else if (errorOutput != null && !errorOutput.isBlank()) {
                    detail = detail + System.lineSeparator() + errorOutput;
                }
                throw new BusinessException("CT 推理失败: " + detail);
            }

            return objectMapper.readValue(extractJsonPayload(output), B1B2InferenceResult.class);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("调用 CT 推理脚本失败: " + ex.getMessage());
        }
    }

    private String extractJsonPayload(String output) {
        if (output == null || output.isBlank()) {
            throw new BusinessException("CT 推理脚本未输出结果");
        }
        int jsonStart = output.indexOf('{');
        int jsonEnd = output.lastIndexOf('}');
        if (jsonStart < 0 || jsonEnd < jsonStart) {
            throw new BusinessException("CT 推理脚本未输出合法 JSON: " + output);
        }
        return output.substring(jsonStart, jsonEnd + 1);
    }
}
