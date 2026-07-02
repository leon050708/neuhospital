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
DELETE FROM visit_queue WHERE id IN (9701);
DELETE FROM registration WHERE id IN (9601, 9602, 9603);
DELETE FROM doctor_schedule WHERE id IN (9401, 9402, 9403, 9404);
DELETE FROM sys_user_role
WHERE user_id IN (
    SELECT id FROM sys_user WHERE username IN (
        'admin_demo',
        'clerk_demo',
        'doctor_demo_01',
        'doctor_demo_02',
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
    'pharmacist_demo',
    '13890000001',
    '13890000002'
);
DELETE FROM doctor WHERE doctor_no IN ('DOC-DEMO-01', 'DOC-DEMO-02', 'DOC-DEMO-03');
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
    (9203, 'DOC-DEMO-03', '赵影', 'FEMALE', '主治医师', 9103, '医学影像演示医生', 'CT 阅片、影像结果解读', '13990000013', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

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
    (9505, 'pharmacist_demo', '$2y$10$UOAWbWnBlnjD1zY4qe39dekNA11C.ediOYefrjx3M9Av4.8CL/L8S', 'MANAGEMENT', NULL, '药房老师', '13990000003', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9506, '13890000001', '$2y$10$UOAWbWnBlnjD1zY4qe39dekNA11C.ediOYefrjx3M9Av4.8CL/L8S', 'PATIENT', 9301, '张演示', '13890000001', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9507, '13890000002', '$2y$10$UOAWbWnBlnjD1zY4qe39dekNA11C.ediOYefrjx3M9Av4.8CL/L8S', 'PATIENT', 9302, '王体验', '13890000002', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

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
WHERE u.username IN ('doctor_demo_01', 'doctor_demo_02')
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
    (9404, 9201, 9101, CURRENT_DATE + 1, 'MORNING', 20, 20, 30.00, 'NORMAL', 'ENABLED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
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

-- 10. 候诊队列
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

-- 11. 药品基础数据
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
