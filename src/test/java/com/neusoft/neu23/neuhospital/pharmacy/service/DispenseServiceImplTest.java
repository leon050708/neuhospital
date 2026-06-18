package com.neusoft.neu23.neuhospital.pharmacy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.pharmacy.dto.DispenseReq;
import com.neusoft.neu23.neuhospital.pharmacy.entity.DrugInfoEntity;
import com.neusoft.neu23.neuhospital.pharmacy.entity.PrescriptionEntity;
import com.neusoft.neu23.neuhospital.pharmacy.entity.PrescriptionItemEntity;
import com.neusoft.neu23.neuhospital.pharmacy.mapper.DrugDispenseRecordMapper;
import com.neusoft.neu23.neuhospital.pharmacy.mapper.DrugInfoMapper;
import com.neusoft.neu23.neuhospital.pharmacy.mapper.PrescriptionItemMapper;
import com.neusoft.neu23.neuhospital.pharmacy.service.impl.DispenseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DispenseServiceImplTest {

    @InjectMocks
    private DispenseServiceImpl dispenseService;

    @Mock
    private PrescriptionService prescriptionService;

    @Mock
    private PrescriptionItemMapper prescriptionItemMapper;

    @Mock
    private DrugInfoMapper drugInfoMapper;

    @Mock
    private DrugDispenseRecordMapper drugDispenseRecordMapper;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(dispenseService, "baseMapper", drugDispenseRecordMapper);
    }

    @Test
    void testDispenseSuccess() {
        // Arrange
        DispenseReq req = new DispenseReq();
        req.setPrescriptionId(1L);
        req.setPharmacyUserId(100L);

        PrescriptionEntity prescription = new PrescriptionEntity();
        prescription.setId(1L);
        prescription.setStatus("PAID");
        prescription.setPatientId(200L);

        when(prescriptionService.getById(1L)).thenReturn(prescription);

        List<PrescriptionItemEntity> items = new ArrayList<>();
        PrescriptionItemEntity item1 = new PrescriptionItemEntity();
        item1.setDrugId(10L);
        item1.setQuantity(2);
        items.add(item1);

        when(prescriptionItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(items);

        // Mock stock deduction success
        when(drugInfoMapper.deductStock(10L, 2)).thenReturn(1);

        // Mock saving dispense record (BaseMapper insert simulation is complex for ServiceImpl standard method `save`, 
        // but `ServiceImpl` uses baseMapper.insert internally)
        when(drugDispenseRecordMapper.insert(any())).thenReturn(1);

        // Act
        Long recordId = dispenseService.dispense(req);

        // Assert
        assertEquals("DISPENSED", prescription.getStatus());
        verify(prescriptionService, times(1)).updateById(prescription);
        verify(drugInfoMapper, times(1)).deductStock(10L, 2);
    }

    @Test
    void testDispenseFailWhenNotPaid() {
        // Arrange
        DispenseReq req = new DispenseReq();
        req.setPrescriptionId(1L);

        PrescriptionEntity prescription = new PrescriptionEntity();
        prescription.setId(1L);
        prescription.setStatus("NEW"); // Not PAID

        when(prescriptionService.getById(1L)).thenReturn(prescription);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            dispenseService.dispense(req);
        });

        assertEquals("该处方未缴费，无法发药", exception.getMessage());
        verify(drugInfoMapper, never()).deductStock(anyLong(), anyInt());
    }

    @Test
    void testDispenseFailWhenStockInsufficient() {
        // Arrange
        DispenseReq req = new DispenseReq();
        req.setPrescriptionId(1L);

        PrescriptionEntity prescription = new PrescriptionEntity();
        prescription.setId(1L);
        prescription.setStatus("PAID");

        when(prescriptionService.getById(1L)).thenReturn(prescription);

        List<PrescriptionItemEntity> items = new ArrayList<>();
        PrescriptionItemEntity item1 = new PrescriptionItemEntity();
        item1.setDrugId(10L);
        item1.setQuantity(2);
        items.add(item1);

        when(prescriptionItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(items);

        // Mock stock deduction failure
        when(drugInfoMapper.deductStock(10L, 2)).thenReturn(0);
        
        DrugInfoEntity drugInfo = new DrugInfoEntity();
        drugInfo.setDrugName("阿莫西林");
        when(drugInfoMapper.selectById(10L)).thenReturn(drugInfo);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            dispenseService.dispense(req);
        });

        assertTrue(exception.getMessage().contains("库存不足"));
        verify(prescriptionService, never()).updateById(any());
    }
}
