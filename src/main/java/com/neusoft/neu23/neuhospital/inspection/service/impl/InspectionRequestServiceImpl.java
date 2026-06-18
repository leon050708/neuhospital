package com.neusoft.neu23.neuhospital.inspection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.inspection.dto.InspectionRequestCreateReq;
import com.neusoft.neu23.neuhospital.inspection.entity.InspectionRequestEntity;
import com.neusoft.neu23.neuhospital.inspection.mapper.InspectionRequestMapper;
import com.neusoft.neu23.neuhospital.inspection.service.InspectionRequestService;
import com.neusoft.neu23.neuhospital.inspection.vo.InspectionRequestVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
public class InspectionRequestServiceImpl extends ServiceImpl<InspectionRequestMapper, InspectionRequestEntity> implements InspectionRequestService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRequest(InspectionRequestCreateReq req) {
        InspectionRequestEntity entity = new InspectionRequestEntity();
        BeanUtils.copyProperties(req, entity);
        entity.setRequestNo("IR" + System.currentTimeMillis());
        entity.setStatus("NEW");
        this.save(entity);
        return entity.getId();
    }

    @Override
    public InspectionRequestVO getRequestDetail(Long id) {
        InspectionRequestEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(404, "检验申请不存在");
        }
        InspectionRequestVO vo = new InspectionRequestVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    @Override
    public Page<InspectionRequestVO> getRequestsPage(Integer pageNo, Integer pageSize, Long patientId, Long doctorId) {
        LambdaQueryWrapper<InspectionRequestEntity> wrapper = new LambdaQueryWrapper<>();
        if (patientId != null) {
            wrapper.eq(InspectionRequestEntity::getPatientId, patientId);
        }
        if (doctorId != null) {
            wrapper.eq(InspectionRequestEntity::getDoctorId, doctorId);
        }
        wrapper.orderByDesc(InspectionRequestEntity::getCreatedAt);

        Page<InspectionRequestEntity> entityPage = new Page<>(pageNo, pageSize);
        this.page(entityPage, wrapper);

        Page<InspectionRequestVO> voPage = new Page<>(pageNo, pageSize, entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(entity -> {
            InspectionRequestVO vo = new InspectionRequestVO();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        }).collect(Collectors.toList()));

        return voPage;
    }
}
