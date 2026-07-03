package com.neusoft.neu23.neuhospital.registration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.neu23.neuhospital.registration.entity.RegistrationMessageLogEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RegistrationMessageLogMapper extends BaseMapper<RegistrationMessageLogEntity> {

    @Insert("""
            INSERT INTO registration_message_log
            (msg_id, schedule_id, patient_id, payload, status, retry_count, create_time, update_time)
            VALUES
            (#{msgId}, #{scheduleId}, #{patientId}, CAST(#{payload} AS json), #{status}, #{retryCount}, #{createTime}, #{updateTime})
            """)
    int insertWithJsonPayload(RegistrationMessageLogEntity entity);
}
