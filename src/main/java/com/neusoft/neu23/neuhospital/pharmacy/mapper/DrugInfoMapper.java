package com.neusoft.neu23.neuhospital.pharmacy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.neu23.neuhospital.pharmacy.entity.DrugInfoEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DrugInfoMapper extends BaseMapper<DrugInfoEntity> {
    
    @org.apache.ibatis.annotations.Update("UPDATE drug_info SET stock_quantity = stock_quantity - #{quantity} WHERE id = #{drugId} AND stock_quantity >= #{quantity} AND deleted = false")
    int deductStock(@org.apache.ibatis.annotations.Param("drugId") Long drugId, @org.apache.ibatis.annotations.Param("quantity") Integer quantity);
}
