package com.neusoft.neu23.neuhospital.registration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.neu23.neuhospital.registration.entity.VisitQueueEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VisitQueueMapper extends BaseMapper<VisitQueueEntity> {
    
    // 获取医生某个时间段内当前最大的排队号
    @Select("SELECT COALESCE(MAX(queue_no), 0) FROM visit_queue v " +
            "JOIN registration r ON v.registration_id = r.id " +
            "WHERE v.doctor_id = #{doctorId} " +
            "AND r.visit_date = CURRENT_DATE " +
            "AND r.time_slot = #{timeSlot}")
    Integer getMaxQueueNo(@Param("doctorId") Long doctorId, @Param("timeSlot") String timeSlot);
}
