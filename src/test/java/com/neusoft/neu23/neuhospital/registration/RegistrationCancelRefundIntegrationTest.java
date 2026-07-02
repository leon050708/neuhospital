package com.neusoft.neu23.neuhospital.registration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.neu23.neuhospital.auth.security.CustomUserDetails;
import com.neusoft.neu23.neuhospital.doctor.entity.DepartmentEntity;
import com.neusoft.neu23.neuhospital.doctor.entity.DoctorEntity;
import com.neusoft.neu23.neuhospital.doctor.mapper.DepartmentMapper;
import com.neusoft.neu23.neuhospital.doctor.mapper.DoctorMapper;
import com.neusoft.neu23.neuhospital.payment.entity.PaymentItemEntity;
import com.neusoft.neu23.neuhospital.payment.entity.PaymentOrderEntity;
import com.neusoft.neu23.neuhospital.payment.entity.RefundRecordEntity;
import com.neusoft.neu23.neuhospital.payment.mapper.PaymentItemMapper;
import com.neusoft.neu23.neuhospital.payment.mapper.PaymentOrderMapper;
import com.neusoft.neu23.neuhospital.payment.mapper.RefundRecordMapper;
import com.neusoft.neu23.neuhospital.patient.entity.PatientEntity;
import com.neusoft.neu23.neuhospital.patient.mapper.PatientMapper;
import com.neusoft.neu23.neuhospital.registration.entity.DoctorScheduleEntity;
import com.neusoft.neu23.neuhospital.registration.entity.RegistrationEntity;
import com.neusoft.neu23.neuhospital.registration.mapper.DoctorScheduleMapper;
import com.neusoft.neu23.neuhospital.registration.mapper.RegistrationMapper;
import com.neusoft.neu23.neuhospital.registration.service.RegistrationService;
import com.neusoft.neu23.neuhospital.system.entity.SysUserEntity;
import com.neusoft.neu23.neuhospital.system.mapper.SysUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
public class RegistrationCancelRefundIntegrationTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private DoctorScheduleMapper doctorScheduleMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private PatientMapper patientMapper;

    @Autowired
    private PaymentOrderMapper paymentOrderMapper;

    @Autowired
    private PaymentItemMapper paymentItemMapper;

    @Autowired
    private RefundRecordMapper refundRecordMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String currentStockKey;

    @AfterEach
    void cleanupContext() {
        SecurityContextHolder.clearContext();
        if (currentStockKey != null) {
            redisTemplate.delete(currentStockKey);
        }
    }

    @Test
    void cancelRegistration_shouldCreateRefundRecordAndUpdatePaymentStatusForPaidRegistration() {
        long suffix = System.nanoTime();
        LocalDateTime now = LocalDateTime.now();

        PatientEntity patient = new PatientEntity();
        patient.setPatientNo("PAT-" + suffix);
        patient.setName("退号测试患者");
        patient.setGender("MALE");
        patient.setBirthDate(LocalDate.of(1995, 1, 1));
        patient.setPhone("138" + String.valueOf(suffix).substring(0, 8));
        patient.setStatus("ENABLED");
        patient.setCreatedAt(now);
        patient.setUpdatedAt(now);
        patient.setDeleted(false);
        patientMapper.insert(patient);
        Long patientId = patient.getId();

        SysUserEntity operatorUser = new SysUserEntity();
        operatorUser.setUsername("patient-user-" + suffix);
        operatorUser.setPasswordHash("test-password-hash");
        operatorUser.setUserType("PATIENT");
        operatorUser.setBizId(patientId);
        operatorUser.setRealName("退号测试患者");
        operatorUser.setPhone("137" + String.valueOf(suffix).substring(0, 8));
        operatorUser.setStatus("ENABLED");
        operatorUser.setCreatedAt(now);
        operatorUser.setUpdatedAt(now);
        operatorUser.setDeleted(false);
        sysUserMapper.insert(operatorUser);
        Long operatorUserId = operatorUser.getId();

        DepartmentEntity department = new DepartmentEntity();
        department.setDeptCode("TEST-DEPT-" + suffix);
        department.setDeptName("退号测试科室");
        department.setDeptType("OUTPATIENT");
        department.setStatus("ENABLED");
        department.setCreatedAt(now);
        department.setUpdatedAt(now);
        department.setDeleted(false);
        departmentMapper.insert(department);

        DoctorEntity doctor = new DoctorEntity();
        doctor.setDoctorNo("TEST-DR-" + suffix);
        doctor.setName("退号测试医生");
        doctor.setGender("MALE");
        doctor.setTitle("ATTENDING");
        doctor.setDepartmentId(department.getId());
        doctor.setPhone("139" + String.valueOf(suffix).substring(0, 8));
        doctor.setStatus("ENABLED");
        doctor.setCreatedAt(now);
        doctor.setUpdatedAt(now);
        doctor.setDeleted(false);
        doctorMapper.insert(doctor);

        DoctorScheduleEntity schedule = new DoctorScheduleEntity();
        schedule.setDoctorId(doctor.getId());
        schedule.setDepartmentId(department.getId());
        schedule.setScheduleDate(LocalDate.now().plusDays(1));
        schedule.setTimeSlot("AM");
        schedule.setSourceCount(20);
        schedule.setAvailableCount(3);
        schedule.setFeeAmount(new BigDecimal("30.00"));
        schedule.setStatus("ENABLED");
        schedule.setCreatedAt(now);
        schedule.setUpdatedAt(now);
        schedule.setDeleted(false);
        doctorScheduleMapper.insert(schedule);
        currentStockKey = "schedule:stock:" + schedule.getId();
        redisTemplate.opsForValue().set(currentStockKey, String.valueOf(schedule.getAvailableCount()));

        RegistrationEntity registration = new RegistrationEntity();
        registration.setRegistrationNo("REG-CANCEL-" + suffix);
        registration.setPatientId(patientId);
        registration.setDoctorId(schedule.getDoctorId());
        registration.setDepartmentId(schedule.getDepartmentId());
        registration.setScheduleId(schedule.getId());
        registration.setVisitDate(schedule.getScheduleDate());
        registration.setTimeSlot(schedule.getTimeSlot());
        registration.setStatus("PAID");
        registration.setFeeAmount(new BigDecimal("30.00"));
        registration.setRegisteredAt(now);
        registration.setCreatedAt(now);
        registration.setUpdatedAt(now);
        registration.setDeleted(false);
        registrationMapper.insert(registration);

        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setOrderNo("PAY-CANCEL-" + suffix);
        order.setPatientId(patientId);
        order.setRegistrationId(registration.getId());
        order.setOrderType("OUTPATIENT");
        order.setTotalAmount(new BigDecimal("30.00"));
        order.setPaidAmount(new BigDecimal("30.00"));
        order.setPayStatus("PAID");
        order.setPayChannel("MOCK_WECHAT");
        order.setPayTime(now);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order.setDeleted(false);
        paymentOrderMapper.insert(order);

        PaymentItemEntity item = new PaymentItemEntity();
        item.setPaymentOrderId(order.getId());
        item.setItemType("REGISTRATION");
        item.setBizId(registration.getId());
        item.setItemName("挂号费-" + registration.getRegistrationNo());
        item.setUnitPrice(new BigDecimal("30.00"));
        item.setQuantity(1);
        item.setAmount(new BigDecimal("30.00"));
        item.setStatus("PAID");
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        item.setDeleted(false);
        paymentItemMapper.insert(item);

        CustomUserDetails principal = new CustomUserDetails(operatorUserId, "patient-test", "PATIENT", "PATIENT", patientId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        registrationService.cancelRegistration(registration.getId(), patientId);

        RegistrationEntity savedRegistration = registrationMapper.selectById(registration.getId());
        assertEquals("CANCELLED", savedRegistration.getStatus());
        assertEquals("患者主动退号", savedRegistration.getCancelReason());

        DoctorScheduleEntity savedSchedule = doctorScheduleMapper.selectById(schedule.getId());
        assertEquals(4, savedSchedule.getAvailableCount());
        assertEquals("4", redisTemplate.opsForValue().get(currentStockKey));

        PaymentItemEntity savedItem = paymentItemMapper.selectById(item.getId());
        assertEquals("REFUNDED", savedItem.getStatus());

        PaymentOrderEntity savedOrder = paymentOrderMapper.selectById(order.getId());
        assertEquals("REFUNDED", savedOrder.getPayStatus());

        List<RefundRecordEntity> refundRecords = refundRecordMapper.selectList(
                new LambdaQueryWrapper<RefundRecordEntity>()
                        .eq(RefundRecordEntity::getPaymentOrderId, order.getId())
        );
        assertEquals(1, refundRecords.size());
        RefundRecordEntity refundRecord = refundRecords.get(0);
        assertEquals(new BigDecimal("30.00"), refundRecord.getRefundAmount());
        assertEquals("患者主动退号", refundRecord.getRefundReason());
        assertEquals(operatorUserId, refundRecord.getOperatorId());
        assertEquals("SUCCESS", refundRecord.getStatus());
        assertNotNull(refundRecord.getRefundNo());
        assertFalse(refundRecord.getRefundNo().isBlank());
    }
}
