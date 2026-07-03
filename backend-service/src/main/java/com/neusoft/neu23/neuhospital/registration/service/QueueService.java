package com.neusoft.neu23.neuhospital.registration.service;

import com.neusoft.neu23.neuhospital.registration.vo.QueueItemVO;
import java.util.List;

public interface QueueService {
    /**
     * 获取医生的待诊队列
     */
    List<QueueItemVO> getDoctorQueue(Long doctorId);

    /**
     * 叫号
     * @param id visit_queue.id
     */
    void callPatient(Long id, Long doctorId);

    /**
     * 过号
     * @param id visit_queue.id
     */
    void skipPatient(Long id, Long doctorId);

    /**
     * 完成接诊
     * @param id visit_queue.id
     */
    void finishPatient(Long id, Long doctorId);
}
