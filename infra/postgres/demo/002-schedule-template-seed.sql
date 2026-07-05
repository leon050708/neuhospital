-- 演示排班模板：覆盖 9201-9208 八位演示医生
-- 执行前需已运行 001-demo-seed.sql 与 004-schedule-template.sql

INSERT INTO doctor_schedule_template
    (doctor_id, department_id, day_of_week, time_slot, source_count, fee_amount, source_type, status, deleted)
VALUES
    -- 9201 李明 神经内科
    (9201, 9101, 1, 'MORNING', 20, 30.00, 'NORMAL', 'ENABLED', FALSE),
    (9201, 9101, 3, 'MORNING', 20, 30.00, 'NORMAL', 'ENABLED', FALSE),
    (9201, 9101, 5, 'MORNING', 20, 30.00, 'NORMAL', 'ENABLED', FALSE),
    -- 9202 王敏 心内科
    (9202, 9102, 2, 'MORNING', 15, 50.00, 'EXPERT', 'ENABLED', FALSE),
    (9202, 9102, 4, 'MORNING', 15, 50.00, 'EXPERT', 'ENABLED', FALSE),
    -- 9203 赵影 影像科
    (9203, 9103, 1, 'AFTERNOON', 10, 80.00, 'EXPERT', 'ENABLED', FALSE),
    (9203, 9103, 3, 'MORNING', 10, 80.00, 'EXPERT', 'ENABLED', FALSE),
    -- 9204 陈涛 神经内科专家
    (9204, 9101, 2, 'AFTERNOON', 12, 60.00, 'EXPERT', 'ENABLED', FALSE),
    (9204, 9101, 4, 'AFTERNOON', 12, 60.00, 'EXPERT', 'ENABLED', FALSE),
    -- 9205 孙悦 心内科
    (9205, 9102, 1, 'AFTERNOON', 18, 40.00, 'NORMAL', 'ENABLED', FALSE),
    (9205, 9102, 5, 'AFTERNOON', 18, 40.00, 'NORMAL', 'ENABLED', FALSE),
    -- 9206 周宁 影像科
    (9206, 9103, 3, 'MORNING', 10, 100.00, 'EXPERT', 'ENABLED', FALSE),
    (9206, 9103, 5, 'AFTERNOON', 10, 100.00, 'EXPERT', 'ENABLED', FALSE),
    -- 9207 高琳 检验科
    (9207, 9104, 2, 'MORNING', 20, 25.00, 'NORMAL', 'ENABLED', FALSE),
    (9207, 9104, 4, 'MORNING', 20, 25.00, 'NORMAL', 'ENABLED', FALSE),
    -- 9208 何川 神经内科
    (9208, 9101, 1, 'AFTERNOON', 18, 30.00, 'NORMAL', 'ENABLED', FALSE),
    (9208, 9101, 4, 'MORNING', 18, 30.00, 'NORMAL', 'ENABLED', FALSE)
ON CONFLICT (doctor_id, day_of_week, time_slot) DO UPDATE
SET department_id = EXCLUDED.department_id,
    source_count = EXCLUDED.source_count,
    fee_amount = EXCLUDED.fee_amount,
    source_type = EXCLUDED.source_type,
    status = EXCLUDED.status,
    deleted = FALSE,
    updated_at = CURRENT_TIMESTAMP;
