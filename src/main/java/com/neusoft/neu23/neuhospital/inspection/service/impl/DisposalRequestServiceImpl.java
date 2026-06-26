package com.neusoft.neu23.neuhospital.inspection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.inspection.dto.DisposalRequestCreateReq;
import com.neusoft.neu23.neuhospital.inspection.entity.DisposalRequestEntity;
import com.neusoft.neu23.neuhospital.inspection.mapper.DisposalRequestMapper;
import com.neusoft.neu23.neuhospital.inspection.service.DisposalRequestService;
import com.neusoft.neu23.neuhospital.inspection.vo.DisposalRequestVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class DisposalRequestServiceImpl extends ServiceImpl<DisposalRequestMapper, DisposalRequestEntity> implements DisposalRequestService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRequest(DisposalRequestCreateReq req) {
        DisposalRequestEntity entity = new DisposalRequestEntity();
        BeanUtils.copyProperties(req, entity);
        entity.setRequestNo("DR" + System.currentTimeMillis());
        entity.setStatus("NEW");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        this.save(entity);
        return entity.getId();
    }

    @Override
    public DisposalRequestVO getRequestDetail(Long id) {
        DisposalRequestEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(404, "处置申请不存在");
        }
        return toVO(entity);
    }

    @Override
    public Page<DisposalRequestVO> getRequestsPage(Integer pageNo, Integer pageSize, Long patientId, Long doctorId) {
        LambdaQueryWrapper<DisposalRequestEntity> wrapper = new LambdaQueryWrapper<>();
        if (patientId != null) {
            wrapper.eq(DisposalRequestEntity::getPatientId, patientId);
        }
        if (doctorId != null) {
            wrapper.eq(DisposalRequestEntity::getDoctorId, doctorId);
        }
        wrapper.orderByDesc(DisposalRequestEntity::getCreatedAt);

        Page<DisposalRequestEntity> entityPage = new Page<>(pageNo, pageSize);
        this.page(entityPage, wrapper);

        Page<DisposalRequestVO> voPage = new Page<>(pageNo, pageSize, entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelRequest(Long id) {
        DisposalRequestEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(404, "处置申请不存在");
        }
        if (!"NEW".equals(entity.getStatus())) {
            throw new BusinessException(400, "只有新建状态的处置申请可以取消");
        }
        entity.setStatus("CANCELLED");
        entity.setUpdatedAt(LocalDateTime.now());
        this.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishRequest(Long id) {
        DisposalRequestEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(404, "处置申请不存在");
        }
        if (!"NEW".equals(entity.getStatus()) && !"EXECUTING".equals(entity.getStatus())) {
            throw new BusinessException(400, "当前状态不可完成处置");
        }
        entity.setStatus("FINISHED");
        entity.setUpdatedAt(LocalDateTime.now());
        this.updateById(entity);
    }

    private DisposalRequestVO toVO(DisposalRequestEntity entity) {
        DisposalRequestVO vo = new DisposalRequestVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
