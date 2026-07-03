BEGIN;

-- 1. 标准角色：如果不存在则补齐，已存在则刷新为启用状态
INSERT INTO sys_role (role_code, role_name, description, status, deleted, created_at, updated_at)
VALUES
    ('ADMIN', '系统管理员', '演示环境管理员角色', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MANAGEMENT', '管理人员', '演示环境管理角色', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('REGISTRATION_CLERK', '挂号员', '演示环境挂号收费角色', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DOCTOR', '医生', '演示环境医生角色', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('PATIENT', '患者', '演示环境患者角色', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('PHARMACIST', '药师', '演示环境药房角色', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (role_code) DO UPDATE
SET role_name = EXCLUDED.role_name,
    description = EXCLUDED.description,
    status = 'ENABLED',
    deleted = FALSE,
    updated_at = CURRENT_TIMESTAMP;

-- 2. 清理旧的演示数据，保证脚本可重复执行
DELETE FROM inspection_result_item
WHERE inspection_result_id IN (
    SELECT ir.id
    FROM inspection_result ir
    JOIN inspection_request req ON req.id = ir.inspection_request_id
    WHERE req.registration_id IN (9601, 9602, 9603)
)
OR id IN (9941, 9942, 9943, 9944, 9945, 9946, 9947, 9948, 9949, 9950, 9951, 9952, 9953, 9954);

DELETE FROM inspection_result
WHERE inspection_request_id IN (
    SELECT id FROM inspection_request WHERE registration_id IN (9601, 9602, 9603)
)
OR id IN (9931, 9932, 9933, 9934);

DELETE FROM check_result
WHERE check_request_id IN (
    SELECT id FROM check_request WHERE registration_id IN (9601, 9602, 9603)
)
OR id IN (9921, 9922, 9923, 9924);

DELETE FROM refund_record
WHERE payment_order_id IN (
    SELECT id FROM payment_order WHERE registration_id IN (9601, 9602, 9603)
);

DELETE FROM payment_item
WHERE payment_order_id IN (
    SELECT id FROM payment_order WHERE registration_id IN (9601, 9602, 9603)
);

DELETE FROM drug_dispense_record
WHERE prescription_id IN (
    SELECT id FROM prescription WHERE registration_id IN (9601, 9602, 9603)
);

DELETE FROM prescription_item
WHERE prescription_id IN (
    SELECT id FROM prescription WHERE registration_id IN (9601, 9602, 9603)
);

DELETE FROM prescription
WHERE registration_id IN (9601, 9602, 9603);

DELETE FROM payment_order
WHERE registration_id IN (9601, 9602, 9603);

DELETE FROM disposal_request
WHERE registration_id IN (9601, 9602, 9603);

DELETE FROM visit_queue
WHERE registration_id IN (9601, 9602, 9603)
OR id IN (9701);

DELETE FROM check_request
WHERE registration_id IN (9601, 9602, 9603)
OR id IN (9901, 9902, 9903, 9904, 9905);

DELETE FROM inspection_request
WHERE registration_id IN (9601, 9602, 9603)
OR id IN (9911, 9912, 9913, 9914, 9915);

DELETE FROM medical_record
WHERE registration_id IN (9601, 9602, 9603);

DELETE FROM registration
WHERE id IN (9601, 9602, 9603);
DELETE FROM doctor_schedule WHERE id IN (9401, 9402, 9403, 9404, 9405, 9406, 9407, 9408, 9409, 9410, 9411, 9412);
DELETE FROM sys_user_role
WHERE user_id IN (
    SELECT id FROM sys_user WHERE username IN (
        'admin_demo',
        'clerk_demo',
        'doctor_demo_01',
        'doctor_demo_02',
        'doctor_demo_03',
        'doctor_demo_04',
        'doctor_demo_05',
        'doctor_demo_06',
        'doctor_demo_07',
        'doctor_demo_08',
        'pharmacist_demo',
        '13890000001',
        '13890000002'
    )
);
DELETE FROM sys_user
WHERE username IN (
    'admin_demo',
    'clerk_demo',
    'doctor_demo_01',
    'doctor_demo_02',
    'doctor_demo_03',
    'doctor_demo_04',
    'doctor_demo_05',
    'doctor_demo_06',
    'doctor_demo_07',
    'doctor_demo_08',
    'pharmacist_demo',
    '13890000001',
    '13890000002'
);
DELETE FROM doctor WHERE doctor_no IN (
    'DOC-DEMO-01', 'DOC-DEMO-02', 'DOC-DEMO-03', 'DOC-DEMO-04',
    'DOC-DEMO-05', 'DOC-DEMO-06', 'DOC-DEMO-07', 'DOC-DEMO-08'
);
DELETE FROM department WHERE dept_code IN ('DEMO-NEURO', 'DEMO-CARD', 'DEMO-IMG', 'DEMO-LAB', 'DEMO-PHARM');
DELETE FROM patient WHERE patient_no IN ('PAT-DEMO-01', 'PAT-DEMO-02');
DELETE FROM drug_info WHERE drug_code IN ('DRUG-DEMO-01', 'DRUG-DEMO-02', 'DRUG-DEMO-03', 'DRUG-DEMO-04');

-- 3. 科室
INSERT INTO department (id, dept_code, dept_name, dept_type, description, status, deleted, created_at, updated_at)
VALUES
    (9101, 'DEMO-NEURO', '神经内科演示', 'OUTPATIENT', '用于门诊、排班、挂号和病历演示', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9102, 'DEMO-CARD', '心内科演示', 'OUTPATIENT', '用于多科室挂号演示', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9103, 'DEMO-IMG', '医学影像科演示', 'IMAGING', '用于检查申请与 CT 演示', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9104, 'DEMO-LAB', '检验科演示', 'INSPECTION', '用于检验申请演示', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9105, 'DEMO-PHARM', '药房演示', 'PHARMACY', '用于药品与发药演示', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 4. 医生
INSERT INTO doctor (
    id, doctor_no, name, gender, title, department_id, introduction, specialty, phone, status, deleted, created_at, updated_at
)
VALUES
    (9201, 'DOC-DEMO-01', '李明', 'MALE', '主任医师', 9101, '神经内科门诊演示医生', '头痛、眩晕、脑血管病', '13990000011', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9202, 'DOC-DEMO-02', '王敏', 'FEMALE', '副主任医师', 9102, '心内科门诊演示医生', '高血压、心律失常', '13990000012', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9203, 'DOC-DEMO-03', '赵影', 'FEMALE', '主治医师', 9103, '医学影像演示医生', 'CT 阅片、影像结果解读', '13990000013', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9204, 'DOC-DEMO-04', '陈涛', 'MALE', '副主任医师', 9101, '神经内科演示专家门诊医生', '脑卒中、癫痫、周围神经病', '13990000014', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9205, 'DOC-DEMO-05', '孙悦', 'FEMALE', '主治医师', 9102, '心内科年轻骨干医生', '冠心病、胸痛、心功能不全', '13990000015', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9206, 'DOC-DEMO-06', '周宁', 'MALE', '主任医师', 9103, '影像科资深演示医生', '头颅 CT、肺部 CT、影像报告审核', '13990000016', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9207, 'DOC-DEMO-07', '高琳', 'FEMALE', '主治医师', 9104, '检验科演示医生', '血常规、生化检验、感染指标解读', '13990000017', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9208, 'DOC-DEMO-08', '何川', 'MALE', '副主任医师', 9101, '神经内科门诊演示医生', '失眠、焦虑相关躯体症状、记忆减退', '13990000018', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 5. 患者
INSERT INTO patient (
    id, patient_no, name, gender, birth_date, phone, id_card, blood_type, allergy_summary, history_summary,
    emergency_contact, emergency_phone, status, deleted, created_at, updated_at
)
VALUES
    (9301, 'PAT-DEMO-01', '张演示', 'MALE', DATE '1995-05-12', '13890000001', '210102199505120011', 'A', '青霉素过敏', '既往偏头痛病史',
     '张家属', '13890010001', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9302, 'PAT-DEMO-02', '王体验', 'FEMALE', DATE '1988-09-21', '13890000002', '210102198809210022', 'O', NULL, '既往高血压病史',
     '王家属', '13890010002', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 6. 演示账号
-- password123 的 BCrypt 哈希
INSERT INTO sys_user (
    id, username, password_hash, user_type, biz_id, real_name, phone, status, deleted, created_at, updated_at
)
VALUES
    (9501, 'admin_demo', '$2y$10$UOAWbWnBlnjD1zY4qe39dekNA11C.ediOYefrjx3M9Av4.8CL/L8S', 'MANAGEMENT', NULL, '系统管理员', '13990000001', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9502, 'clerk_demo', '$2y$10$UOAWbWnBlnjD1zY4qe39dekNA11C.ediOYefrjx3M9Av4.8CL/L8S', 'MANAGEMENT', NULL, '挂号收费员', '13990000002', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9503, 'doctor_demo_01', '$2y$10$UOAWbWnBlnjD1zY4qe39dekNA11C.ediOYefrjx3M9Av4.8CL/L8S', 'DOCTOR', 9201, '李明', '13990000011', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9504, 'doctor_demo_02', '$2y$10$UOAWbWnBlnjD1zY4qe39dekNA11C.ediOYefrjx3M9Av4.8CL/L8S', 'DOCTOR', 9202, '王敏', '13990000012', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9505, 'doctor_demo_03', '$2y$10$UOAWbWnBlnjD1zY4qe39dekNA11C.ediOYefrjx3M9Av4.8CL/L8S', 'DOCTOR', 9203, '赵影', '13990000013', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9506, 'doctor_demo_04', '$2y$10$UOAWbWnBlnjD1zY4qe39dekNA11C.ediOYefrjx3M9Av4.8CL/L8S', 'DOCTOR', 9204, '陈涛', '13990000014', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9507, 'doctor_demo_05', '$2y$10$UOAWbWnBlnjD1zY4qe39dekNA11C.ediOYefrjx3M9Av4.8CL/L8S', 'DOCTOR', 9205, '孙悦', '13990000015', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9508, 'doctor_demo_06', '$2y$10$UOAWbWnBlnjD1zY4qe39dekNA11C.ediOYefrjx3M9Av4.8CL/L8S', 'DOCTOR', 9206, '周宁', '13990000016', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9509, 'doctor_demo_07', '$2y$10$UOAWbWnBlnjD1zY4qe39dekNA11C.ediOYefrjx3M9Av4.8CL/L8S', 'DOCTOR', 9207, '高琳', '13990000017', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9510, 'doctor_demo_08', '$2y$10$UOAWbWnBlnjD1zY4qe39dekNA11C.ediOYefrjx3M9Av4.8CL/L8S', 'DOCTOR', 9208, '何川', '13990000018', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9511, 'pharmacist_demo', '$2y$10$UOAWbWnBlnjD1zY4qe39dekNA11C.ediOYefrjx3M9Av4.8CL/L8S', 'MANAGEMENT', NULL, '药房老师', '13990000003', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9512, '13890000001', '$2y$10$UOAWbWnBlnjD1zY4qe39dekNA11C.ediOYefrjx3M9Av4.8CL/L8S', 'PATIENT', 9301, '张演示', '13890000001', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9513, '13890000002', '$2y$10$UOAWbWnBlnjD1zY4qe39dekNA11C.ediOYefrjx3M9Av4.8CL/L8S', 'PATIENT', 9302, '王体验', '13890000002', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 7. 账号角色关系
INSERT INTO sys_user_role (user_id, role_id, created_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP
FROM sys_user u
JOIN sys_role r ON r.role_code = 'ADMIN'
WHERE u.username = 'admin_demo'
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO sys_user_role (user_id, role_id, created_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP
FROM sys_user u
JOIN sys_role r ON r.role_code = 'REGISTRATION_CLERK'
WHERE u.username = 'clerk_demo'
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO sys_user_role (user_id, role_id, created_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP
FROM sys_user u
JOIN sys_role r ON r.role_code = 'DOCTOR'
WHERE u.username IN (
    'doctor_demo_01', 'doctor_demo_02', 'doctor_demo_03', 'doctor_demo_04',
    'doctor_demo_05', 'doctor_demo_06', 'doctor_demo_07', 'doctor_demo_08'
)
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO sys_user_role (user_id, role_id, created_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP
FROM sys_user u
JOIN sys_role r ON r.role_code = 'PHARMACIST'
WHERE u.username = 'pharmacist_demo'
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO sys_user_role (user_id, role_id, created_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP
FROM sys_user u
JOIN sys_role r ON r.role_code = 'PATIENT'
WHERE u.username IN ('13890000001', '13890000002')
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 8. 排班
INSERT INTO doctor_schedule (
    id, doctor_id, department_id, schedule_date, time_slot, source_count, available_count, fee_amount, source_type, status, deleted, created_at, updated_at
)
VALUES
    (9401, 9201, 9101, CURRENT_DATE, 'MORNING', 20, 18, 30.00, 'NORMAL', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9402, 9201, 9101, CURRENT_DATE, 'AFTERNOON', 20, 20, 30.00, 'NORMAL', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9403, 9202, 9102, CURRENT_DATE, 'MORNING', 15, 15, 50.00, 'EXPERT', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9404, 9201, 9101, CURRENT_DATE + 1, 'MORNING', 20, 20, 30.00, 'NORMAL', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9405, 9204, 9101, CURRENT_DATE, 'AFTERNOON', 12, 10, 60.00, 'EXPERT', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9406, 9208, 9101, CURRENT_DATE + 1, 'AFTERNOON', 18, 18, 30.00, 'NORMAL', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9407, 9205, 9102, CURRENT_DATE, 'AFTERNOON', 18, 18, 40.00, 'NORMAL', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9408, 9202, 9102, CURRENT_DATE + 1, 'MORNING', 15, 15, 50.00, 'EXPERT', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9409, 9203, 9103, CURRENT_DATE, 'MORNING', 10, 10, 80.00, 'EXPERT', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9410, 9206, 9103, CURRENT_DATE + 1, 'AFTERNOON', 10, 10, 100.00, 'EXPERT', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9411, 9207, 9104, CURRENT_DATE, 'MORNING', 20, 20, 25.00, 'NORMAL', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9412, 9207, 9104, CURRENT_DATE + 1, 'MORNING', 20, 20, 25.00, 'NORMAL', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE
SET doctor_id = EXCLUDED.doctor_id,
    department_id = EXCLUDED.department_id,
    schedule_date = EXCLUDED.schedule_date,
    time_slot = EXCLUDED.time_slot,
    source_count = EXCLUDED.source_count,
    available_count = EXCLUDED.available_count,
    fee_amount = EXCLUDED.fee_amount,
    source_type = EXCLUDED.source_type,
    status = EXCLUDED.status,
    deleted = FALSE,
    updated_at = CURRENT_TIMESTAMP;

-- 9. 演示挂号单
-- 9601: 患者张演示今天下午未支付挂号，可直接用于支付与签到演示
-- 9602: 患者王体验今天上午已签到，医生登录后可直接看到候诊队列
-- 9603: 张演示明日上午已挂号未支付，可用于“我的挂号”展示
INSERT INTO registration (
    id, registration_no, patient_id, doctor_id, department_id, schedule_id, visit_date, time_slot, queue_no,
    source_type, status, fee_amount, cancel_reason, registered_at, deleted, created_at, updated_at
)
VALUES
    (9601, 'REG-DEMO-UNPAID', 9301, 9201, 9101, 9402, CURRENT_DATE, 'AFTERNOON', NULL, 'NORMAL', 'UNPAID', 30.00, NULL, CURRENT_TIMESTAMP, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9602, 'REG-DEMO-QUEUE', 9302, 9201, 9101, 9401, CURRENT_DATE, 'MORNING', 1, 'NORMAL', 'IN_PROGRESS', 30.00, NULL, CURRENT_TIMESTAMP, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9603, 'REG-DEMO-TOMORROW', 9301, 9201, 9101, 9404, CURRENT_DATE + 1, 'MORNING', NULL, 'NORMAL', 'UNPAID', 30.00, NULL, CURRENT_TIMESTAMP, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE
SET registration_no = EXCLUDED.registration_no,
    patient_id = EXCLUDED.patient_id,
    doctor_id = EXCLUDED.doctor_id,
    department_id = EXCLUDED.department_id,
    schedule_id = EXCLUDED.schedule_id,
    visit_date = EXCLUDED.visit_date,
    time_slot = EXCLUDED.time_slot,
    queue_no = EXCLUDED.queue_no,
    source_type = EXCLUDED.source_type,
    status = EXCLUDED.status,
    fee_amount = EXCLUDED.fee_amount,
    cancel_reason = EXCLUDED.cancel_reason,
    registered_at = EXCLUDED.registered_at,
    deleted = FALSE,
    updated_at = CURRENT_TIMESTAMP;

-- 10. 演示检查申请
-- 9901: 患者张演示的头颅 CT，适合医生端和 CT/检查页展示
-- 9902: 患者王体验的心电图申请，适合已签到患者的门诊流程展示
-- 9903: 患者张演示的胸部 DR，保留 NEW 状态便于支付页展示
-- 9904/9905: 追加胸部 CT、腹部彩超，方便医生端列表和结果页联调
INSERT INTO check_request (
    id, request_no, patient_id, registration_id, medical_record_id, doctor_id, department_id, target_department_id,
    check_item_code, check_item_name, clinical_diagnosis, purpose, urgent_flag, fee_amount,
    status, result_summary, requested_at, created_at, updated_at, deleted
)
VALUES
    (9901, 'CHK-DEMO-CT-01', 9301, 9601, NULL, 9201, 9101, 9103,
     'CT_HEAD', '头颅 CT 平扫', '反复头痛待查', '排查脑出血及占位病变', FALSE, 180.00,
     'REPORTED', '影像提示未见急性脑出血征象', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9902, 'CHK-DEMO-ECG-01', 9302, 9602, NULL, 9202, 9102, 9103,
     'ECG_12', '十二导联心电图', '心悸伴胸闷', '评估心律失常风险', TRUE, 45.00,
     'EXECUTING', '检查已完成，报告草稿待确认', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9903, 'CHK-DEMO-DR-01', 9301, 9603, NULL, 9201, 9101, 9103,
     'DR_CHEST', '胸部 DR', '咳嗽伴胸痛', '排查肺部感染或胸腔积液', FALSE, 80.00,
     'NEW', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9904, 'CHK-DEMO-CT-02', 9302, 9602, NULL, 9202, 9102, 9103,
     'CT_CHEST', '胸部 CT 平扫', '发热伴气促', '评估肺部感染范围', TRUE, 220.00,
     'REPORTED', '双肺散在炎性改变，未见明显胸腔积液', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9905, 'CHK-DEMO-US-01', 9301, 9601, NULL, 9201, 9101, 9103,
     'US_ABDOMEN', '腹部彩超', '上腹不适', '筛查肝胆胰脾肾结构异常', FALSE, 120.00,
     'PAID', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)
ON CONFLICT (id) DO UPDATE
SET request_no = EXCLUDED.request_no,
    patient_id = EXCLUDED.patient_id,
    registration_id = EXCLUDED.registration_id,
    medical_record_id = EXCLUDED.medical_record_id,
    doctor_id = EXCLUDED.doctor_id,
    department_id = EXCLUDED.department_id,
    target_department_id = EXCLUDED.target_department_id,
    check_item_code = EXCLUDED.check_item_code,
    check_item_name = EXCLUDED.check_item_name,
    clinical_diagnosis = EXCLUDED.clinical_diagnosis,
    purpose = EXCLUDED.purpose,
    urgent_flag = EXCLUDED.urgent_flag,
    fee_amount = EXCLUDED.fee_amount,
    status = EXCLUDED.status,
    result_summary = EXCLUDED.result_summary,
    requested_at = EXCLUDED.requested_at,
    deleted = FALSE,
    updated_at = CURRENT_TIMESTAMP;

-- 11. 演示检查结果
INSERT INTO check_result (
    id, check_request_id, report_no, result_text, result_summary, conclusion, report_file_id, report_doctor_id,
    reported_at, status, created_at, updated_at, deleted
)
VALUES
    (9921, 9901, 'RPT-DEMO-CT-01',
     '颅脑平扫未见明显高密度出血灶，中线结构居中，脑室系统形态尚可。',
     '影像提示未见急性脑出血征象',
     '建议结合临床症状继续随诊，必要时复查 MRI。',
     NULL, 9203, CURRENT_TIMESTAMP, 'CONFIRMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9922, 9902, 'RPT-DEMO-ECG-01',
     '窦性心律，偶发室性早搏，未见持续性心律失常证据。',
     '存在偶发室早，建议结合动态心电图进一步评估',
     '目前无危急值，建议门诊继续随访。',
     NULL, 9206, CURRENT_TIMESTAMP, 'DRAFT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9923, 9904, 'RPT-DEMO-CT-02',
     '双肺下叶可见斑片状稍高密度影，纵隔未见明显肿大淋巴结。',
     '考虑双肺散在炎性改变',
     '建议结合血常规及感染指标，必要时抗感染后复查胸部 CT。',
     NULL, 9206, CURRENT_TIMESTAMP, 'CONFIRMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9924, 9905, 'RPT-DEMO-US-01',
     '肝胆胰脾肾形态大小未见明显异常，胆囊壁欠光滑。',
     '腹部主要实质器官未见明确占位',
     '建议结合临床症状随诊，注意清淡饮食。',
     NULL, 9203, CURRENT_TIMESTAMP, 'CONFIRMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)
ON CONFLICT (id) DO UPDATE
SET check_request_id = EXCLUDED.check_request_id,
    report_no = EXCLUDED.report_no,
    result_text = EXCLUDED.result_text,
    result_summary = EXCLUDED.result_summary,
    conclusion = EXCLUDED.conclusion,
    report_file_id = EXCLUDED.report_file_id,
    report_doctor_id = EXCLUDED.report_doctor_id,
    reported_at = EXCLUDED.reported_at,
    status = EXCLUDED.status,
    deleted = FALSE,
    updated_at = CURRENT_TIMESTAMP;

-- 12. 演示检验申请
-- 9911/9912/9913/9914/9915 适合患者待缴费、医生申请列表、检验科查看场景
INSERT INTO inspection_request (
    id, request_no, patient_id, registration_id, medical_record_id, doctor_id, department_id, target_department_id,
    inspection_item_code, inspection_item_name, sample_type, urgent_flag, fee_amount,
    status, result_summary, requested_at, created_at, updated_at, deleted
)
VALUES
    (9911, 'INSP-DEMO-BLOOD-01', 9301, 9601, NULL, 9201, 9101, 9104,
     'CBC', '血常规', '静脉血', FALSE, 35.00,
     'NEW', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9912, 'INSP-DEMO-BIO-01', 9302, 9602, NULL, 9202, 9102, 9104,
     'BIOCHEM', '肝肾功能', '静脉血', FALSE, 120.00,
     'REPORTED', '肝肾功能总体正常，ALT 轻度升高', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9913, 'INSP-DEMO-TROP-01', 9302, 9602, NULL, 9202, 9102, 9104,
     'TROPONIN', '肌钙蛋白', '静脉血', TRUE, 95.00,
     'EXECUTING', '检验结果草稿已生成，待医生复核', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9914, 'INSP-DEMO-CRP-01', 9301, 9601, NULL, 9201, 9101, 9104,
     'CRP', 'C 反应蛋白', '静脉血', TRUE, 42.00,
     'REPORTED', 'CRP 升高，提示存在活动性炎症', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9915, 'INSP-DEMO-DDIMER-01', 9302, 9602, NULL, 9202, 9102, 9104,
     'D_DIMER', 'D-二聚体', '静脉血', TRUE, 68.00,
     'PAID', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)
ON CONFLICT (id) DO UPDATE
SET request_no = EXCLUDED.request_no,
    patient_id = EXCLUDED.patient_id,
    registration_id = EXCLUDED.registration_id,
    medical_record_id = EXCLUDED.medical_record_id,
    doctor_id = EXCLUDED.doctor_id,
    department_id = EXCLUDED.department_id,
    target_department_id = EXCLUDED.target_department_id,
    inspection_item_code = EXCLUDED.inspection_item_code,
    inspection_item_name = EXCLUDED.inspection_item_name,
    sample_type = EXCLUDED.sample_type,
    urgent_flag = EXCLUDED.urgent_flag,
    fee_amount = EXCLUDED.fee_amount,
    status = EXCLUDED.status,
    result_summary = EXCLUDED.result_summary,
    requested_at = EXCLUDED.requested_at,
    deleted = FALSE,
    updated_at = CURRENT_TIMESTAMP;

-- 13. 演示检验结果
INSERT INTO inspection_result (
    id, inspection_request_id, report_no, summary, conclusion, report_file_id, report_doctor_id,
    reported_at, status, created_at, updated_at, deleted
)
VALUES
    (9931, 9912, 'INSPR-DEMO-01',
     '肝肾功能总体正常，ALT 轻度升高，其余指标在参考范围内。',
     '建议清淡饮食，1-2 周后复查肝功能。',
     NULL, 9207, CURRENT_TIMESTAMP, 'CONFIRMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9932, 9913, 'INSPR-DEMO-02',
     '肌钙蛋白结果轻度升高，建议结合症状及心电图复核。',
     '当前建议尽快由临床医生进一步评估心肌损伤风险。',
     NULL, 9207, CURRENT_TIMESTAMP, 'DRAFT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9933, 9914, 'INSPR-DEMO-03',
     'CRP 明显升高，支持体内存在炎症反应。',
     '建议结合血常规、影像结果及体温变化综合判断感染情况。',
     NULL, 9207, CURRENT_TIMESTAMP, 'CONFIRMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9934, 9915, 'INSPR-DEMO-04',
     'D-二聚体样本已接收，结果待出具。',
     '当前为草稿占位结果，便于前端联调。',
     NULL, 9207, CURRENT_TIMESTAMP, 'DRAFT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)
ON CONFLICT (id) DO UPDATE
SET inspection_request_id = EXCLUDED.inspection_request_id,
    report_no = EXCLUDED.report_no,
    summary = EXCLUDED.summary,
    conclusion = EXCLUDED.conclusion,
    report_file_id = EXCLUDED.report_file_id,
    report_doctor_id = EXCLUDED.report_doctor_id,
    reported_at = EXCLUDED.reported_at,
    status = EXCLUDED.status,
    deleted = FALSE,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO inspection_result_item (
    id, inspection_result_id, item_code, item_name, result_value, unit, reference_range, abnormal_flag,
    created_at, updated_at, deleted
)
VALUES
    (9941, 9931, 'ALT', '丙氨酸氨基转移酶', '58', 'U/L', '7-40', 'HIGH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9942, 9931, 'AST', '天门冬氨酸氨基转移酶', '32', 'U/L', '13-35', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9943, 9931, 'CREA', '肌酐', '79', 'umol/L', '57-111', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9944, 9932, 'TNI', '肌钙蛋白 I', '0.18', 'ng/mL', '0-0.04', 'HIGH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9945, 9932, 'CKMB', '肌酸激酶同工酶', '28', 'U/L', '0-25', 'HIGH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9946, 9932, 'MYO', '肌红蛋白', '92', 'ng/mL', '0-70', 'HIGH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9947, 9933, 'CRP', 'C 反应蛋白', '36.5', 'mg/L', '0-8', 'HIGH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9948, 9933, 'WBC', '白细胞计数', '11.8', '10^9/L', '3.5-9.5', 'HIGH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9949, 9933, 'NEUT', '中性粒细胞百分比', '79', '%', '40-75', 'HIGH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9950, 9934, 'DDIMER', 'D-二聚体', '0.86', 'mg/L FEU', '0-0.55', 'HIGH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9951, 9934, 'PT', '凝血酶原时间', '12.4', 's', '9-13', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9952, 9934, 'APTT', '活化部分凝血活酶时间', '31.0', 's', '25-35', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9953, 9931, 'UREA', '尿素', '5.1', 'mmol/L', '2.8-7.2', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
    (9954, 9932, 'BNP', '脑钠肽', '145', 'pg/mL', '0-100', 'HIGH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)
ON CONFLICT (id) DO UPDATE
SET inspection_result_id = EXCLUDED.inspection_result_id,
    item_code = EXCLUDED.item_code,
    item_name = EXCLUDED.item_name,
    result_value = EXCLUDED.result_value,
    unit = EXCLUDED.unit,
    reference_range = EXCLUDED.reference_range,
    abnormal_flag = EXCLUDED.abnormal_flag,
    deleted = FALSE,
    updated_at = CURRENT_TIMESTAMP;

-- 14. 候诊队列
INSERT INTO visit_queue (
    id, registration_id, doctor_id, queue_no, queue_status, called_at, finished_at, deleted, created_at, updated_at
)
VALUES
    (9701, 9602, 9201, 1, 'WAITING', NULL, NULL, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE
SET registration_id = EXCLUDED.registration_id,
    doctor_id = EXCLUDED.doctor_id,
    queue_no = EXCLUDED.queue_no,
    queue_status = EXCLUDED.queue_status,
    called_at = EXCLUDED.called_at,
    finished_at = EXCLUDED.finished_at,
    deleted = FALSE,
    updated_at = CURRENT_TIMESTAMP;

-- 15. 药品基础数据
INSERT INTO drug_info (
    id, drug_code, drug_name, generic_name, specification, unit, category, manufacturer, sale_price,
    stock_quantity, warning_quantity, contraindication, status, deleted, created_at, updated_at
)
VALUES
    (9801, 'DRUG-DEMO-01', '阿司匹林肠溶片', '阿司匹林', '100mg*30片', '盒', '心脑血管', '演示制药A', 18.50, 200, 20, '胃溃疡患者慎用', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9802, 'DRUG-DEMO-02', '布洛芬缓释胶囊', '布洛芬', '0.3g*24粒', '盒', '止痛退热', '演示制药B', 22.00, 150, 20, '消化道出血患者慎用', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9803, 'DRUG-DEMO-03', '阿莫西林胶囊', '阿莫西林', '0.25g*50粒', '盒', '抗生素', '演示制药C', 15.50, 180, 20, '青霉素过敏禁用', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9804, 'DRUG-DEMO-04', '硝苯地平控释片', '硝苯地平', '30mg*7片', '盒', '降压药', '演示制药D', 28.00, 120, 15, '低血压患者慎用', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE
SET drug_code = EXCLUDED.drug_code,
    drug_name = EXCLUDED.drug_name,
    generic_name = EXCLUDED.generic_name,
    specification = EXCLUDED.specification,
    unit = EXCLUDED.unit,
    category = EXCLUDED.category,
    manufacturer = EXCLUDED.manufacturer,
    sale_price = EXCLUDED.sale_price,
    stock_quantity = EXCLUDED.stock_quantity,
    warning_quantity = EXCLUDED.warning_quantity,
    contraindication = EXCLUDED.contraindication,
    status = EXCLUDED.status,
    deleted = FALSE,
    updated_at = CURRENT_TIMESTAMP;

COMMIT;
