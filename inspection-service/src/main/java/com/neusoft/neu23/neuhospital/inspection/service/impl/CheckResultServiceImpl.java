package com.neusoft.neu23.neuhospital.inspection.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.inspection.dto.CheckResultCreateReq;
import com.neusoft.neu23.neuhospital.inspection.entity.CheckRequestEntity;
import com.neusoft.neu23.neuhospital.inspection.entity.CheckResultEntity;
import com.neusoft.neu23.neuhospital.inspection.mapper.CheckResultMapper;
import com.neusoft.neu23.neuhospital.inspection.service.CheckRequestService;
import com.neusoft.neu23.neuhospital.inspection.service.CheckResultService;
import com.neusoft.neu23.neuhospital.inspection.vo.CheckResultVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CheckResultServiceImpl extends ServiceImpl<CheckResultMapper, CheckResultEntity> implements CheckResultService {

    @Autowired
    private CheckRequestService checkRequestService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordResult(CheckResultCreateReq req) {
        CheckRequestEntity request = checkRequestService.getById(req.getCheckRequestId());
        if (request == null) {
            throw new BusinessException(404, "检查申请不存在");
        }
        // 状态机校验：必须是PAID或EXECUTING才能录入结果
        if (!"PAID".equals(request.getStatus()) && !"EXECUTING".equals(request.getStatus())) {
            throw new BusinessException(400, "该检查单未缴费或状态异常，无法录入结果");
        }

        CheckResultEntity entity = new CheckResultEntity();
        BeanUtils.copyProperties(req, entity);
        entity.setReportNo("RPT" + System.currentTimeMillis());
        entity.setStatus("DRAFT");
        this.save(entity);

        // 更新申请单状态为执行中
        request.setStatus("EXECUTING");
        checkRequestService.updateById(request);

        return entity.getId();
    }

    @Override
    public CheckResultVO getResultDetail(Long id) {
        CheckResultEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(404, "检查结果不存在");
        }
        CheckResultVO vo = new CheckResultVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmResult(Long id) {
        CheckResultEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(404, "检查结果不存在");
        }
        if ("CONFIRMED".equals(entity.getStatus())) {
            throw new BusinessException(400, "结果已确认");
        }
        entity.setStatus("CONFIRMED");
        entity.setReportedAt(LocalDateTime.now());
        this.updateById(entity);

        // 更新申请单状态为已出报告
        CheckRequestEntity request = checkRequestService.getById(entity.getCheckRequestId());
        request.setStatus("REPORTED");
        request.setResultSummary(entity.getResultSummary());
        checkRequestService.updateById(request);
    }
}
