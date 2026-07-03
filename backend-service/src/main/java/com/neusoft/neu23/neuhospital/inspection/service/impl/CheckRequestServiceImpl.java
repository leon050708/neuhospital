package com.neusoft.neu23.neuhospital.inspection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.inspection.dto.CheckRequestCreateReq;
import com.neusoft.neu23.neuhospital.inspection.entity.CheckRequestEntity;
import com.neusoft.neu23.neuhospital.inspection.mapper.CheckRequestMapper;
import com.neusoft.neu23.neuhospital.inspection.service.CheckRequestService;
import com.neusoft.neu23.neuhospital.inspection.vo.CheckRequestVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
public class CheckRequestServiceImpl extends ServiceImpl<CheckRequestMapper, CheckRequestEntity> implements CheckRequestService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRequest(CheckRequestCreateReq req) {
        CheckRequestEntity entity = new CheckRequestEntity();
        BeanUtils.copyProperties(req, entity);
        entity.setRequestNo("CR" + System.currentTimeMillis());
        entity.setStatus("NEW"); // 初始状态为新建，需等待缴费
        this.save(entity);
        return entity.getId();
    }

    @Override
    public CheckRequestVO getRequestDetail(Long id) {
        CheckRequestEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(404, "检查申请不存在");
        }
        CheckRequestVO vo = new CheckRequestVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    @Override
    public Page<CheckRequestVO> getRequestsPage(Integer pageNo, Integer pageSize, Long patientId, Long doctorId) {
        LambdaQueryWrapper<CheckRequestEntity> wrapper = new LambdaQueryWrapper<>();
        if (patientId != null) {
            wrapper.eq(CheckRequestEntity::getPatientId, patientId);
        }
        if (doctorId != null) {
            wrapper.eq(CheckRequestEntity::getDoctorId, doctorId);
        }
        wrapper.orderByDesc(CheckRequestEntity::getCreatedAt);

        Page<CheckRequestEntity> entityPage = new Page<>(pageNo, pageSize);
        this.page(entityPage, wrapper);

        Page<CheckRequestVO> voPage = new Page<>(pageNo, pageSize, entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(entity -> {
            CheckRequestVO vo = new CheckRequestVO();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        }).collect(Collectors.toList()));

        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelRequest(Long id) {
        CheckRequestEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(404, "检查申请不存在");
        }
        if (!"NEW".equals(entity.getStatus())) {
            throw new BusinessException(400, "只有新建状态的申请可以取消");
        }
        entity.setStatus("CANCELLED");
        this.updateById(entity);
    }
}
