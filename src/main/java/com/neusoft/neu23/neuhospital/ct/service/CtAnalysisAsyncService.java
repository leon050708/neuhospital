package com.neusoft.neu23.neuhospital.ct.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.neu23.neuhospital.ct.entity.CtAnalysisResultEntity;
import com.neusoft.neu23.neuhospital.ct.entity.CtAnalysisTaskEntity;
import com.neusoft.neu23.neuhospital.ct.mapper.CtAnalysisResultMapper;
import com.neusoft.neu23.neuhospital.ct.mapper.CtAnalysisTaskMapper;
import com.neusoft.neu23.neuhospital.ct.vo.B1B2InferenceResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class CtAnalysisAsyncService {

    private final CtAnalysisTaskMapper ctAnalysisTaskMapper;
    private final CtAnalysisResultMapper ctAnalysisResultMapper;
    private final CtAiInferenceService ctAiInferenceService;
    private final ObjectMapper objectMapper;

    public CtAnalysisAsyncService(CtAnalysisTaskMapper ctAnalysisTaskMapper,
                                  CtAnalysisResultMapper ctAnalysisResultMapper,
                                  CtAiInferenceService ctAiInferenceService,
                                  ObjectMapper objectMapper) {
        this.ctAnalysisTaskMapper = ctAnalysisTaskMapper;
        this.ctAnalysisResultMapper = ctAnalysisResultMapper;
        this.ctAiInferenceService = ctAiInferenceService;
        this.objectMapper = objectMapper;
    }

    @Async
    public void executeTask(Long taskId) {
        CtAnalysisTaskEntity task = ctAnalysisTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }

        task.setTaskStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        ctAnalysisTaskMapper.updateById(task);

        try {
            B1B2InferenceResult inferenceResult = ctAiInferenceService.infer(task.getInputPath());

            CtAnalysisResultEntity resultEntity = ctAnalysisResultMapper.selectOne(
                    new QueryWrapper<CtAnalysisResultEntity>().eq("task_id", taskId).last("limit 1"));
            if (resultEntity == null) {
                resultEntity = new CtAnalysisResultEntity();
                resultEntity.setTaskId(taskId);
                resultEntity.setCreatedAt(LocalDateTime.now());
            }

            resultEntity.setAnalysisType(inferenceResult.getAnalysisType());
            resultEntity.setPredictedCategory(inferenceResult.getPredictedCategory());
            resultEntity.setConfidence(toDecimal(inferenceResult.getConfidence()));
            resultEntity.setProbabilityB1(toDecimal(inferenceResult.getClassProbabilities().get("B1")));
            resultEntity.setProbabilityB2(toDecimal(inferenceResult.getClassProbabilities().get("B2")));
            resultEntity.setRiskLevel(inferenceResult.getRiskLevel());
            resultEntity.setModelName(inferenceResult.getModelName());
            resultEntity.setDoctorConfirmStatus("UNCONFIRMED");
            resultEntity.setRawResultJson(toJson(inferenceResult));
            resultEntity.setUpdatedAt(LocalDateTime.now());

            if (resultEntity.getId() == null) {
                ctAnalysisResultMapper.insert(resultEntity);
            } else {
                ctAnalysisResultMapper.updateById(resultEntity);
            }

            task.setTaskStatus("SUCCESS");
            task.setFailureReason(null);
        } catch (Exception ex) {
            task.setTaskStatus("FAILED");
            task.setFailureReason(ex.getMessage());
        }

        task.setFinishedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        ctAnalysisTaskMapper.updateById(task);
    }

    private BigDecimal toDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private String toJson(B1B2InferenceResult inferenceResult) {
        try {
            return objectMapper.writeValueAsString(inferenceResult);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
