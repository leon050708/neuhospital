package com.neusoft.neu23.neuhospital.pharmacy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.neu23.neuhospital.pharmacy.dto.DispenseReq;
import com.neusoft.neu23.neuhospital.pharmacy.entity.DrugDispenseRecordEntity;

public interface DispenseService extends IService<DrugDispenseRecordEntity> {
    Long dispense(DispenseReq req);
}
