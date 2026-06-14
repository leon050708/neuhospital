package com.neusoft.neu23.neuhospital.registration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.neu23.neuhospital.registration.entity.DoctorScheduleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DoctorScheduleMapper extends BaseMapper<DoctorScheduleEntity> {
    
    // 乐观锁扣减可用号源，防超卖
    @Update("UPDATE doctor_schedule SET available_count = available_count - 1 WHERE id = #{id} AND available_count > 0")
    int deductAvailableCount(@Param("id") Long id);
}
