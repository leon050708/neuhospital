package com.neusoft.neu23.neuhospital.pharmacy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.pharmacy.dto.PrescriptionCreateReq;
import com.neusoft.neu23.neuhospital.pharmacy.entity.DrugInfoEntity;
import com.neusoft.neu23.neuhospital.pharmacy.entity.PrescriptionEntity;
import com.neusoft.neu23.neuhospital.pharmacy.entity.PrescriptionItemEntity;
import com.neusoft.neu23.neuhospital.pharmacy.mapper.DrugInfoMapper;
import com.neusoft.neu23.neuhospital.pharmacy.mapper.PrescriptionItemMapper;
import com.neusoft.neu23.neuhospital.pharmacy.mapper.PrescriptionMapper;
import com.neusoft.neu23.neuhospital.pharmacy.service.PrescriptionService;
import com.neusoft.neu23.neuhospital.pharmacy.vo.PrescriptionItemVO;
import com.neusoft.neu23.neuhospital.pharmacy.vo.PrescriptionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PrescriptionServiceImpl extends ServiceImpl<PrescriptionMapper, PrescriptionEntity> implements PrescriptionService {

    @Autowired
    private PrescriptionItemMapper prescriptionItemMapper;

    @Autowired
    private DrugInfoMapper drugInfoMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPrescription(PrescriptionCreateReq req) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BusinessException(400, "处方明细不能为空");
        }

        PrescriptionEntity entity = new PrescriptionEntity();
        BeanUtils.copyProperties(req, entity);
        entity.setPrescriptionNo("RX" + System.currentTimeMillis());
        entity.setStatus("NEW");
        entity.setIssuedAt(LocalDateTime.now());

        BigDecimal totalAmount = BigDecimal.ZERO;
        this.save(entity);

        for (var itemReq : req.getItems()) {
            DrugInfoEntity drug = drugInfoMapper.selectById(itemReq.getDrugId());
            if (drug == null) {
                throw new BusinessException(404, "药品不存在，ID: " + itemReq.getDrugId());
            }

            PrescriptionItemEntity itemEntity = new PrescriptionItemEntity();
            BeanUtils.copyProperties(itemReq, itemEntity);
            itemEntity.setPrescriptionId(entity.getId());
            itemEntity.setDrugName(drug.getDrugName());
            itemEntity.setSpecification(drug.getSpecification());
            itemEntity.setUnitPrice(drug.getSalePrice());
            
            BigDecimal amount = drug.getSalePrice().multiply(new BigDecimal(itemReq.getQuantity()));
            itemEntity.setAmount(amount);
            prescriptionItemMapper.insert(itemEntity);

            totalAmount = totalAmount.add(amount);
        }

        entity.setTotalAmount(totalAmount);
        this.updateById(entity);

        return entity.getId();
    }

    @Override
    public PrescriptionVO getPrescriptionDetail(Long id) {
        PrescriptionEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(404, "处方不存在");
        }
        PrescriptionVO vo = new PrescriptionVO();
        BeanUtils.copyProperties(entity, vo);

        List<PrescriptionItemEntity> items = prescriptionItemMapper.selectList(
                new LambdaQueryWrapper<PrescriptionItemEntity>().eq(PrescriptionItemEntity::getPrescriptionId, id)
        );

        vo.setItems(items.stream().map(item -> {
            PrescriptionItemVO itemVO = new PrescriptionItemVO();
            BeanUtils.copyProperties(item, itemVO);
            return itemVO;
        }).collect(Collectors.toList()));

        return vo;
    }

    @Override
    public Page<PrescriptionVO> getPrescriptionsPage(Integer pageNo, Integer pageSize, Long patientId, Long doctorId) {
        LambdaQueryWrapper<PrescriptionEntity> wrapper = new LambdaQueryWrapper<>();
        if (patientId != null) {
            wrapper.eq(PrescriptionEntity::getPatientId, patientId);
        }
        if (doctorId != null) {
            wrapper.eq(PrescriptionEntity::getDoctorId, doctorId);
        }
        wrapper.orderByDesc(PrescriptionEntity::getCreatedAt);

        Page<PrescriptionEntity> entityPage = new Page<>(pageNo, pageSize);
        this.page(entityPage, wrapper);

        Page<PrescriptionVO> voPage = new Page<>(pageNo, pageSize, entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(entity -> {
            PrescriptionVO vo = new PrescriptionVO();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        }).collect(Collectors.toList()));

        return voPage;
    }
}
