package com.neusoft.neu23.neuhospital.registration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.neu23.neuhospital.registration.entity.RegistrationEntity;
import com.neusoft.neu23.neuhospital.registration.entity.VisitQueueEntity;
import com.neusoft.neu23.neuhospital.registration.mapper.RegistrationMapper;
import com.neusoft.neu23.neuhospital.registration.mapper.VisitQueueMapper;
import com.neusoft.neu23.neuhospital.registration.service.QueueService;
import com.neusoft.neu23.neuhospital.registration.vo.QueueItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class QueueServiceImpl implements QueueService {

    @Autowired
    private VisitQueueMapper visitQueueMapper;

    @Autowired
    private RegistrationMapper registrationMapper;

    @Override
    public List<QueueItemVO> getDoctorQueue(Long doctorId) {
        QueryWrapper<VisitQueueEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("doctor_id", doctorId);
        wrapper.in("queue_status", "WAITING", "CALLED", "SKIPPED");
        wrapper.orderByAsc("queue_no");
        
        List<VisitQueueEntity> entities = visitQueueMapper.selectList(wrapper);
        List<QueueItemVO> vos = new ArrayList<>();
        for (VisitQueueEntity e : entities) {
            QueueItemVO vo = new QueueItemVO();
            vo.setId(e.getId());
            vo.setRegistrationId(e.getRegistrationId());
            vo.setQueueNo(e.getQueueNo());
            vo.setQueueStatus(e.getQueueStatus());
            vo.setCalledAt(e.getCalledAt());

            RegistrationEntity reg = registrationMapper.selectById(e.getRegistrationId());
            if (reg != null) {
                vo.setPatientId(reg.getPatientId());
                vo.setRegistrationNo(reg.getRegistrationNo());
                vo.setRegisteredAt(reg.getRegisteredAt());
            }
            vos.add(vo);
        }
        
        vos.sort((a, b) -> {
            int scoreA = getStatusScore(a.getQueueStatus());
            int scoreB = getStatusScore(b.getQueueStatus());
            if (scoreA != scoreB) return scoreA - scoreB;
            return a.getQueueNo() - b.getQueueNo();
        });
        
        return vos;
    }

    private int getStatusScore(String status) {
        if ("CALLED".equals(status)) return 1;
        if ("WAITING".equals(status)) return 2;
        if ("SKIPPED".equals(status)) return 3;
        return 99;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void callPatient(Long id, Long doctorId) {
        VisitQueueEntity queue = visitQueueMapper.selectById(id);
        if (queue == null || !queue.getDoctorId().equals(doctorId)) {
            throw new RuntimeException("队列信息不存在或无权操作");
        }
        
        // 将正在接诊的标记为过号（或者由医生主动结束）
        QueryWrapper<VisitQueueEntity> updateWrapper = new QueryWrapper<>();
        updateWrapper.eq("doctor_id", doctorId).eq("queue_status", "CALLED");
        VisitQueueEntity currentCalled = visitQueueMapper.selectOne(updateWrapper);
        if (currentCalled != null && !currentCalled.getId().equals(id)) {
            currentCalled.setQueueStatus("SKIPPED");
            currentCalled.setUpdatedAt(LocalDateTime.now());
            visitQueueMapper.updateById(currentCalled);
        }

        queue.setQueueStatus("CALLED");
        queue.setCalledAt(LocalDateTime.now());
        queue.setUpdatedAt(LocalDateTime.now());
        visitQueueMapper.updateById(queue);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void skipPatient(Long id, Long doctorId) {
        VisitQueueEntity queue = visitQueueMapper.selectById(id);
        if (queue == null || !queue.getDoctorId().equals(doctorId)) {
            throw new RuntimeException("队列信息不存在或无权操作");
        }
        queue.setQueueStatus("SKIPPED");
        queue.setUpdatedAt(LocalDateTime.now());
        visitQueueMapper.updateById(queue);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishPatient(Long id, Long doctorId) {
        VisitQueueEntity queue = visitQueueMapper.selectById(id);
        if (queue == null || !queue.getDoctorId().equals(doctorId)) {
            throw new RuntimeException("队列信息不存在或无权操作");
        }
        queue.setQueueStatus("FINISHED");
        queue.setFinishedAt(LocalDateTime.now());
        queue.setUpdatedAt(LocalDateTime.now());
        visitQueueMapper.updateById(queue);

        RegistrationEntity reg = registrationMapper.selectById(queue.getRegistrationId());
        if (reg != null) {
            reg.setStatus("COMPLETED");
            reg.setUpdatedAt(LocalDateTime.now());
            registrationMapper.updateById(reg);
        }
    }
}
