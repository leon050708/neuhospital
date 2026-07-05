package com.neusoft.neu23.neuhospital.inspection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.inspection.dto.InspectionResultCreateReq;
import com.neusoft.neu23.neuhospital.inspection.entity.InspectionRequestEntity;
import com.neusoft.neu23.neuhospital.inspection.entity.InspectionResultEntity;
import com.neusoft.neu23.neuhospital.inspection.entity.InspectionResultItemEntity;
import com.neusoft.neu23.neuhospital.inspection.mapper.InspectionResultItemMapper;
import com.neusoft.neu23.neuhospital.inspection.mapper.InspectionResultMapper;
import com.neusoft.neu23.neuhospital.inspection.service.InspectionRequestService;
import com.neusoft.neu23.neuhospital.inspection.service.InspectionResultService;
import com.neusoft.neu23.neuhospital.inspection.vo.InspectionResultItemVO;
import com.neusoft.neu23.neuhospital.inspection.vo.InspectionResultVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InspectionResultServiceImpl extends ServiceImpl<InspectionResultMapper, InspectionResultEntity> implements InspectionResultService {

    @Autowired
    private InspectionRequestService inspectionRequestService;

    @Autowired
    private InspectionResultItemMapper inspectionResultItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordResult(InspectionResultCreateReq req) {
        InspectionRequestEntity request = inspectionRequestService.getById(req.getInspectionRequestId());
        if (request == null) {
            throw new BusinessException(404, "检验申请不存在");
        }
        if (!"PAID".equals(request.getStatus()) && !"EXECUTING".equals(request.getStatus())) {
            throw new BusinessException(400, "该检验单未缴费或状态异常，无法录入结果");
        }

        InspectionResultEntity entity = new InspectionResultEntity();
        BeanUtils.copyProperties(req, entity);
        entity.setSummary(req.getResultSummary()); // Manually map resultSummary to summary
        entity.setReportNo("INSP" + System.currentTimeMillis());
        entity.setStatus("DRAFT");
        entity.setReportedAt(LocalDateTime.now());
        this.save(entity);

        if (req.getItems() != null && !req.getItems().isEmpty()) {
            for (var itemReq : req.getItems()) {
                InspectionResultItemEntity item = new InspectionResultItemEntity();
                BeanUtils.copyProperties(itemReq, item);
                item.setInspectionResultId(entity.getId());
                inspectionResultItemMapper.insert(item);
            }
        }

        request.setStatus("EXECUTING");
        request.setResultSummary(entity.getSummary());
        inspectionRequestService.updateById(request);

        return entity.getId();
    }

    @Override
    public InspectionResultVO getResultDetail(Long id) {
        InspectionResultEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(404, "检验结果不存在");
        }
        InspectionResultVO vo = new InspectionResultVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setResultSummary(entity.getSummary()); // Manually map summary to resultSummary

        List<InspectionResultItemEntity> items = inspectionResultItemMapper.selectList(
                new LambdaQueryWrapper<InspectionResultItemEntity>().eq(InspectionResultItemEntity::getInspectionResultId, id)
        );

        vo.setItems(items.stream().map(item -> {
            InspectionResultItemVO itemVO = new InspectionResultItemVO();
            BeanUtils.copyProperties(item, itemVO);
            return itemVO;
        }).collect(Collectors.toList()));

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmResult(Long id) {
        InspectionResultEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(404, "检验结果不存在");
        }
        if ("CONFIRMED".equals(entity.getStatus())) {
            throw new BusinessException(400, "结果已确认");
        }
        entity.setStatus("CONFIRMED");
        entity.setReportedAt(LocalDateTime.now());
        this.updateById(entity);

        InspectionRequestEntity request = inspectionRequestService.getById(entity.getInspectionRequestId());
        if (request != null) {
            request.setStatus("REPORTED");
            request.setResultSummary(entity.getSummary());
            inspectionRequestService.updateById(request);
        }
    }
}
