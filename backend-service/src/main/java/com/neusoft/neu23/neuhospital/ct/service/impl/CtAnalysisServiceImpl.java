package com.neusoft.neu23.neuhospital.ct.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.ct.dto.CtAnalysisTaskCreateReq;
import com.neusoft.neu23.neuhospital.ct.entity.CtAnalysisResultEntity;
import com.neusoft.neu23.neuhospital.ct.entity.CtAnalysisTaskEntity;
import com.neusoft.neu23.neuhospital.ct.mapper.CtAnalysisResultMapper;
import com.neusoft.neu23.neuhospital.ct.mapper.CtAnalysisTaskMapper;
import com.neusoft.neu23.neuhospital.ct.service.CtAnalysisAsyncService;
import com.neusoft.neu23.neuhospital.ct.service.CtInputResolverService;
import com.neusoft.neu23.neuhospital.ct.service.CtAnalysisService;
import com.neusoft.neu23.neuhospital.ct.vo.CtAnalysisResultVO;
import com.neusoft.neu23.neuhospital.ct.vo.CtAnalysisTaskVO;
import com.neusoft.neu23.neuhospital.file.entity.FileRecordEntity;
import com.neusoft.neu23.neuhospital.file.mapper.FileRecordMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CtAnalysisServiceImpl implements CtAnalysisService {

    private static final String ANALYSIS_TYPE_B1_B2 = "B1_B2_CLASSIFICATION";

    private final CtAnalysisTaskMapper ctAnalysisTaskMapper;
    private final CtAnalysisResultMapper ctAnalysisResultMapper;
    private final FileRecordMapper fileRecordMapper;
    private final CtAnalysisAsyncService ctAnalysisAsyncService;
    private final CtInputResolverService ctInputResolverService;

    public CtAnalysisServiceImpl(CtAnalysisTaskMapper ctAnalysisTaskMapper,
                                 CtAnalysisResultMapper ctAnalysisResultMapper,
                                 FileRecordMapper fileRecordMapper,
                                 CtAnalysisAsyncService ctAnalysisAsyncService,
                                 CtInputResolverService ctInputResolverService) {
        this.ctAnalysisTaskMapper = ctAnalysisTaskMapper;
        this.ctAnalysisResultMapper = ctAnalysisResultMapper;
        this.fileRecordMapper = fileRecordMapper;
        this.ctAnalysisAsyncService = ctAnalysisAsyncService;
        this.ctInputResolverService = ctInputResolverService;
    }

    @Override
    public CtAnalysisTaskVO createTask(CtAnalysisTaskCreateReq req) {
        if (req.getCtImageFileId() == null) {
            throw new BusinessException("ctImageFileId 不能为空");
        }
        if (!ANALYSIS_TYPE_B1_B2.equals(req.getAnalysisType())) {
            throw new BusinessException("当前仅支持 B1_B2_CLASSIFICATION");
        }

        FileRecordEntity fileRecordEntity = fileRecordMapper.selectById(req.getCtImageFileId());
        if (fileRecordEntity == null) {
            throw new BusinessException("CT 文件不存在");
        }

        CtAnalysisTaskEntity taskEntity = new CtAnalysisTaskEntity();
        taskEntity.setCtImageFileId(req.getCtImageFileId());
        taskEntity.setAnalysisType(req.getAnalysisType());
        taskEntity.setTaskStatus("PENDING");
        taskEntity.setSubmittedAt(LocalDateTime.now());
        taskEntity.setCreatedAt(LocalDateTime.now());
        taskEntity.setUpdatedAt(LocalDateTime.now());
        ctAnalysisTaskMapper.insert(taskEntity);

        String inputPath = ctInputResolverService.resolveToLocalPath(taskEntity.getId(), fileRecordEntity);
        taskEntity.setInputPath(inputPath);
        taskEntity.setUpdatedAt(LocalDateTime.now());
        ctAnalysisTaskMapper.updateById(taskEntity);

        ctAnalysisAsyncService.executeTask(taskEntity.getId());
        return convertTask(taskEntity);
    }

    @Override
    public CtAnalysisResultVO getResult(Long taskId) {
        CtAnalysisTaskEntity taskEntity = ctAnalysisTaskMapper.selectById(taskId);
        if (taskEntity == null) {
            throw new BusinessException("CT 分析任务不存在");
        }

        CtAnalysisResultEntity resultEntity = ctAnalysisResultMapper.selectOne(
                new QueryWrapper<CtAnalysisResultEntity>().eq("task_id", taskId).last("limit 1"));

        CtAnalysisResultVO vo = new CtAnalysisResultVO();
        vo.setTaskId(taskEntity.getId());
        vo.setAnalysisType(taskEntity.getAnalysisType());
        vo.setTaskStatus(taskEntity.getTaskStatus());
        vo.setFailureReason(taskEntity.getFailureReason());

        if (resultEntity != null) {
            vo.setPredictedCategory(resultEntity.getPredictedCategory());
            vo.setConfidence(resultEntity.getConfidence());
            Map<String, java.math.BigDecimal> probabilities = new LinkedHashMap<>();
            probabilities.put("B1", resultEntity.getProbabilityB1());
            probabilities.put("B2", resultEntity.getProbabilityB2());
            vo.setClassProbabilities(probabilities);
            vo.setRiskLevel(resultEntity.getRiskLevel());
            vo.setModelName(resultEntity.getModelName());
            vo.setDoctorConfirmStatus(resultEntity.getDoctorConfirmStatus());
        }
        return vo;
    }

    private CtAnalysisTaskVO convertTask(CtAnalysisTaskEntity entity) {
        CtAnalysisTaskVO vo = new CtAnalysisTaskVO();
        vo.setTaskId(entity.getId());
        vo.setCtImageFileId(entity.getCtImageFileId());
        vo.setAnalysisType(entity.getAnalysisType());
        vo.setTaskStatus(entity.getTaskStatus());
        vo.setSubmittedAt(entity.getSubmittedAt());
        return vo;
    }
}
