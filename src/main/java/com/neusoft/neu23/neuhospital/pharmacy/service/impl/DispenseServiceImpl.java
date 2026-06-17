package com.neusoft.neu23.neuhospital.pharmacy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.pharmacy.dto.DispenseReq;
import com.neusoft.neu23.neuhospital.pharmacy.entity.DrugDispenseRecordEntity;
import com.neusoft.neu23.neuhospital.pharmacy.entity.DrugInfoEntity;
import com.neusoft.neu23.neuhospital.pharmacy.entity.PrescriptionEntity;
import com.neusoft.neu23.neuhospital.pharmacy.entity.PrescriptionItemEntity;
import com.neusoft.neu23.neuhospital.pharmacy.mapper.DrugDispenseRecordMapper;
import com.neusoft.neu23.neuhospital.pharmacy.mapper.DrugInfoMapper;
import com.neusoft.neu23.neuhospital.pharmacy.mapper.PrescriptionItemMapper;
import com.neusoft.neu23.neuhospital.pharmacy.service.DispenseService;
import com.neusoft.neu23.neuhospital.pharmacy.service.PrescriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DispenseServiceImpl extends ServiceImpl<DrugDispenseRecordMapper, DrugDispenseRecordEntity> implements DispenseService {

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private PrescriptionItemMapper prescriptionItemMapper;

    @Autowired
    private DrugInfoMapper drugInfoMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long dispense(DispenseReq req) {
        PrescriptionEntity prescription = prescriptionService.getById(req.getPrescriptionId());
        if (prescription == null) {
            throw new BusinessException(404, "处方不存在");
        }
        if (!"PAID".equals(prescription.getStatus())) {
            throw new BusinessException(400, "该处方未缴费，无法发药");
        }

        List<PrescriptionItemEntity> items = prescriptionItemMapper.selectList(
                new LambdaQueryWrapper<PrescriptionItemEntity>().eq(PrescriptionItemEntity::getPrescriptionId, req.getPrescriptionId())
        );

        // 扣减库存 (使用数据库原生的基于版本的悲观锁机制： SELECT ... FOR UPDATE 
        // 但这里我们简单采用乐观锁或者数据库扣减 `update stock = stock - qty where id = ? and stock >= qty`)
        for (PrescriptionItemEntity item : items) {
            // 直接尝试执行带有防超卖条件(stock >= qty)的更新SQL，避免用select再update引发的并发问题
            int updated = drugInfoMapper.deductStock(item.getDrugId(), item.getQuantity());
            if (updated == 0) {
                DrugInfoEntity drugInfo = drugInfoMapper.selectById(item.getDrugId());
                throw new BusinessException(400, "药品 [" + drugInfo.getDrugName() + "] 库存不足，发药失败");
            }
        }

        prescription.setStatus("DISPENSED");
        prescriptionService.updateById(prescription);

        DrugDispenseRecordEntity record = new DrugDispenseRecordEntity();
        record.setPrescriptionId(req.getPrescriptionId());
        record.setPatientId(prescription.getPatientId());
        record.setOperatorId(req.getPharmacyUserId());
        record.setOperateTime(LocalDateTime.now());
        record.setStatus("COMPLETED");
        this.save(record);

        return record.getId();
    }
}
