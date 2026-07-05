package com.neusoft.neu23.neuhospital.pharmacy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.pharmacy.dto.DrugInfoCreateReq;
import com.neusoft.neu23.neuhospital.pharmacy.dto.DrugInfoUpdateReq;
import com.neusoft.neu23.neuhospital.pharmacy.entity.DrugInfoEntity;
import com.neusoft.neu23.neuhospital.pharmacy.mapper.DrugInfoMapper;
import com.neusoft.neu23.neuhospital.pharmacy.service.DrugInfoService;
import com.neusoft.neu23.neuhospital.pharmacy.vo.DrugInfoVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DrugInfoServiceImpl extends ServiceImpl<DrugInfoMapper, DrugInfoEntity> implements DrugInfoService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDrug(DrugInfoCreateReq req) {
        // 校验 drugCode 唯一性
        if (StringUtils.hasText(req.getDrugCode())) {
            boolean exists = this.count(new LambdaQueryWrapper<DrugInfoEntity>()
                    .eq(DrugInfoEntity::getDrugCode, req.getDrugCode())) > 0;
            if (exists) {
                throw new BusinessException(400, "药品编码已存在");
            }
        }
        
        DrugInfoEntity entity = new DrugInfoEntity();
        BeanUtils.copyProperties(req, entity);
        
        if (!StringUtils.hasText(entity.getDrugCode())) {
            entity.setDrugCode("DRUG" + System.currentTimeMillis());
        }
        
        if (entity.getStockQuantity() == null) {
            entity.setStockQuantity(0);
        }
        if (entity.getWarningQuantity() == null) {
            entity.setWarningQuantity(10);
        }
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus("ENABLED");
        }
        
        this.save(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDrug(Long id, DrugInfoUpdateReq req) {
        DrugInfoEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(404, "药品不存在");
        }
        
        // 只能修改基础资料，不允许在此接口修改库存
        BeanUtils.copyProperties(req, entity);
        this.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(Long id, Integer adjustQuantity) {
        if (adjustQuantity == null || adjustQuantity == 0) {
            throw new BusinessException(400, "调整数量不能为 0 或空");
        }
        
        DrugInfoEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(404, "药品不存在");
        }
        
        int newStock = entity.getStockQuantity() + adjustQuantity;
        if (newStock < 0) {
            throw new BusinessException(400, "库存不足，无法扣减");
        }
        
        entity.setStockQuantity(newStock);
        this.updateById(entity);
    }

    @Override
    public DrugInfoVO getDrugById(Long id) {
        DrugInfoEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(404, "药品不存在");
        }
        DrugInfoVO vo = new DrugInfoVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    @Override
    public Page<DrugInfoVO> getDrugsPage(Integer pageNo, Integer pageSize, String keyword, String category) {
        LambdaQueryWrapper<DrugInfoEntity> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.like(DrugInfoEntity::getDrugName, keyword)
                   .or()
                   .like(DrugInfoEntity::getGenericName, keyword)
                   .or()
                   .like(DrugInfoEntity::getDrugCode, keyword);
        }
        
        if (StringUtils.hasText(category)) {
            wrapper.eq(DrugInfoEntity::getCategory, category);
        }
        
        wrapper.orderByDesc(DrugInfoEntity::getCreatedAt);
        
        Page<DrugInfoEntity> entityPage = new Page<>(pageNo, pageSize);
        this.page(entityPage, wrapper);
        
        Page<DrugInfoVO> voPage = new Page<>(pageNo, pageSize, entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(entity -> {
            DrugInfoVO vo = new DrugInfoVO();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        }).toList());
        
        return voPage;
    }
}
