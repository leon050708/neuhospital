package com.neusoft.neu23.neuhospital.pharmacy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.neu23.neuhospital.pharmacy.dto.DrugInfoCreateReq;
import com.neusoft.neu23.neuhospital.pharmacy.dto.DrugInfoUpdateReq;
import com.neusoft.neu23.neuhospital.pharmacy.entity.DrugInfoEntity;
import com.neusoft.neu23.neuhospital.pharmacy.vo.DrugInfoVO;

public interface DrugInfoService extends IService<DrugInfoEntity> {
    
    Long createDrug(DrugInfoCreateReq req);
    
    void updateDrug(Long id, DrugInfoUpdateReq req);
    
    void adjustStock(Long id, Integer adjustQuantity);
    
    DrugInfoVO getDrugById(Long id);
    
    Page<DrugInfoVO> getDrugsPage(Integer pageNo, Integer pageSize, String keyword, String category);
}
