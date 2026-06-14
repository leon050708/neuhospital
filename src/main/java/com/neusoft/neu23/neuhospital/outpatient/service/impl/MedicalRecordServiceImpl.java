package com.neusoft.neu23.neuhospital.outpatient.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.outpatient.dto.MedicalDiagnosisReq;
import com.neusoft.neu23.neuhospital.outpatient.dto.MedicalRecordCreateReq;
import com.neusoft.neu23.neuhospital.outpatient.dto.MedicalRecordUpdateReq;
import com.neusoft.neu23.neuhospital.outpatient.entity.MedicalDiagnosisEntity;
import com.neusoft.neu23.neuhospital.outpatient.entity.MedicalRecordEntity;
import com.neusoft.neu23.neuhospital.outpatient.mapper.MedicalRecordMapper;
import com.neusoft.neu23.neuhospital.outpatient.service.MedicalDiagnosisService;
import com.neusoft.neu23.neuhospital.outpatient.service.MedicalRecordService;
import com.neusoft.neu23.neuhospital.outpatient.vo.MedicalDiagnosisVO;
import com.neusoft.neu23.neuhospital.outpatient.vo.MedicalRecordVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MedicalRecordServiceImpl extends ServiceImpl<MedicalRecordMapper, MedicalRecordEntity> implements MedicalRecordService {

    @Autowired
    private MedicalDiagnosisService diagnosisService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRecord(MedicalRecordCreateReq req) {
        // 校验该挂号单是否已经写过病历
        boolean exists = this.count(new LambdaQueryWrapper<MedicalRecordEntity>()
                .eq(MedicalRecordEntity::getRegistrationId, req.getRegistrationId())) > 0;
        if (exists) {
            throw new BusinessException(400, "该挂号单已关联病历，不可重复创建");
        }

        MedicalRecordEntity entity = new MedicalRecordEntity();
        BeanUtils.copyProperties(req, entity);
        entity.setRecordNo("MR" + System.currentTimeMillis());
        entity.setStatus("DRAFT"); // 初始状态为草稿
        this.save(entity);

        // 如果新建时同时传入了诊断明细
        if (req.getDiagnoses() != null && !req.getDiagnoses().isEmpty()) {
            saveDiagnoses(entity.getId(), req.getDiagnoses());
        }

        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRecord(Long id, MedicalRecordUpdateReq req) {
        MedicalRecordEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(404, "病历不存在");
        }
        if ("CONFIRMED".equals(entity.getStatus())) {
            throw new BusinessException(400, "病历已确认，不可修改");
        }

        BeanUtils.copyProperties(req, entity);
        this.updateById(entity);

        // 如果传入了新的诊断明细，覆盖老的
        if (req.getDiagnoses() != null) {
            saveDiagnoses(id, req.getDiagnoses());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmRecord(Long id) {
        MedicalRecordEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(404, "病历不存在");
        }
        if ("CONFIRMED".equals(entity.getStatus())) {
            throw new BusinessException(400, "病历已经处于确认状态");
        }

        entity.setStatus("CONFIRMED");
        entity.setConfirmedAt(LocalDateTime.now());
        this.updateById(entity);
    }

    @Override
    public MedicalRecordVO getRecordDetail(Long id) {
        MedicalRecordEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(404, "病历不存在");
        }

        MedicalRecordVO vo = new MedicalRecordVO();
        BeanUtils.copyProperties(entity, vo);

        List<MedicalDiagnosisVO> diagnoses = getDiagnosesByRecordId(id);
        vo.setDiagnoses(diagnoses);

        return vo;
    }

    @Override
    public Page<MedicalRecordVO> getRecordsPage(Integer pageNo, Integer pageSize, Long patientId, Long doctorId) {
        LambdaQueryWrapper<MedicalRecordEntity> wrapper = new LambdaQueryWrapper<>();
        if (patientId != null) {
            wrapper.eq(MedicalRecordEntity::getPatientId, patientId);
        }
        if (doctorId != null) {
            wrapper.eq(MedicalRecordEntity::getDoctorId, doctorId);
        }
        wrapper.orderByDesc(MedicalRecordEntity::getCreatedAt);

        Page<MedicalRecordEntity> entityPage = new Page<>(pageNo, pageSize);
        this.page(entityPage, wrapper);

        Page<MedicalRecordVO> voPage = new Page<>(pageNo, pageSize, entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(entity -> {
            MedicalRecordVO vo = new MedicalRecordVO();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        }).collect(Collectors.toList()));

        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDiagnoses(Long recordId, List<MedicalDiagnosisReq> diagnoses) {
        MedicalRecordEntity entity = this.getById(recordId);
        if (entity == null) {
            throw new BusinessException(404, "病历不存在");
        }
        if ("CONFIRMED".equals(entity.getStatus())) {
            throw new BusinessException(400, "病历已确认，不可修改诊断明细");
        }

        // 1. 删除旧的诊断明细
        diagnosisService.remove(new LambdaQueryWrapper<MedicalDiagnosisEntity>()
                .eq(MedicalDiagnosisEntity::getMedicalRecordId, recordId));

        // 2. 插入新的诊断明细
        if (diagnoses != null && !diagnoses.isEmpty()) {
            List<MedicalDiagnosisEntity> entities = diagnoses.stream().map(req -> {
                MedicalDiagnosisEntity diagnosisEntity = new MedicalDiagnosisEntity();
                BeanUtils.copyProperties(req, diagnosisEntity);
                diagnosisEntity.setMedicalRecordId(recordId);
                return diagnosisEntity;
            }).collect(Collectors.toList());
            diagnosisService.saveBatch(entities);
        }
    }

    @Override
    public List<MedicalDiagnosisVO> getDiagnosesByRecordId(Long recordId) {
        List<MedicalDiagnosisEntity> entities = diagnosisService.list(new LambdaQueryWrapper<MedicalDiagnosisEntity>()
                .eq(MedicalDiagnosisEntity::getMedicalRecordId, recordId));
        
        return entities.stream().map(entity -> {
            MedicalDiagnosisVO vo = new MedicalDiagnosisVO();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        }).collect(Collectors.toList());
    }
}
