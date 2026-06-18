CREATE TABLE IF NOT EXISTS doctor_schedule (
    id BIGSERIAL PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    schedule_date DATE NOT NULL,
    time_slot VARCHAR(16) NOT NULL,
    source_count INTEGER NOT NULL,
    available_count INTEGER NOT NULL,
    fee_amount NUMERIC(10, 2) NOT NULL,
    source_type VARCHAR(32),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_doctor_schedule UNIQUE (doctor_id, schedule_date, time_slot),
    CONSTRAINT fk_doctor_schedule_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id),
    CONSTRAINT fk_doctor_schedule_department FOREIGN KEY (department_id) REFERENCES department (id)
);

CREATE INDEX IF NOT EXISTS idx_doctor_schedule_department_date
    ON doctor_schedule (department_id, schedule_date);

CREATE TABLE IF NOT EXISTS registration (
    id BIGSERIAL PRIMARY KEY,
    registration_no VARCHAR(64) NOT NULL UNIQUE,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    schedule_id BIGINT NOT NULL,
    visit_date DATE NOT NULL,
    time_slot VARCHAR(16) NOT NULL,
    queue_no INTEGER,
    source_type VARCHAR(32),
    status VARCHAR(32) NOT NULL,
    fee_amount NUMERIC(10, 2) NOT NULL,
    cancel_reason VARCHAR(255),
    registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_registration_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_registration_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id),
    CONSTRAINT fk_registration_department FOREIGN KEY (department_id) REFERENCES department (id),
    CONSTRAINT fk_registration_schedule FOREIGN KEY (schedule_id) REFERENCES doctor_schedule (id)
);

CREATE INDEX IF NOT EXISTS idx_registration_patient_visit
    ON registration (patient_id, visit_date);

CREATE INDEX IF NOT EXISTS idx_registration_doctor_visit
    ON registration (doctor_id, visit_date);

CREATE TABLE IF NOT EXISTS medical_record (
    id BIGSERIAL PRIMARY KEY,
    record_no VARCHAR(64) NOT NULL UNIQUE,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    registration_id BIGINT NOT NULL,
    chief_complaint TEXT NOT NULL,
    present_illness TEXT,
    past_history TEXT,
    allergy_history TEXT,
    physical_exam TEXT,
    preliminary_diagnosis TEXT,
    final_diagnosis TEXT,
    advice TEXT,
    status VARCHAR(32) NOT NULL,
    confirmed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_medical_record_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_medical_record_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id),
    CONSTRAINT fk_medical_record_department FOREIGN KEY (department_id) REFERENCES department (id),
    CONSTRAINT fk_medical_record_registration FOREIGN KEY (registration_id) REFERENCES registration (id)
);

CREATE INDEX IF NOT EXISTS idx_medical_record_patient
    ON medical_record (patient_id, created_at DESC);

CREATE TABLE IF NOT EXISTS drug_info (
    id BIGSERIAL PRIMARY KEY,
    drug_code VARCHAR(64) NOT NULL UNIQUE,
    drug_name VARCHAR(128) NOT NULL,
    generic_name VARCHAR(128),
    specification VARCHAR(128),
    unit VARCHAR(32),
    category VARCHAR(64),
    manufacturer VARCHAR(128),
    sale_price NUMERIC(10, 2) NOT NULL,
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    warning_quantity INTEGER,
    contraindication TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_drug_info_name
    ON drug_info (drug_name);

CREATE TABLE IF NOT EXISTS prescription (
    id BIGSERIAL PRIMARY KEY,
    prescription_no VARCHAR(64) NOT NULL UNIQUE,
    patient_id BIGINT NOT NULL,
    registration_id BIGINT NOT NULL,
    medical_record_id BIGINT,
    doctor_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    prescription_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    remark VARCHAR(255),
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_prescription_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_prescription_registration FOREIGN KEY (registration_id) REFERENCES registration (id),
    CONSTRAINT fk_prescription_medical_record FOREIGN KEY (medical_record_id) REFERENCES medical_record (id),
    CONSTRAINT fk_prescription_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id),
    CONSTRAINT fk_prescription_department FOREIGN KEY (department_id) REFERENCES department (id)
);

CREATE INDEX IF NOT EXISTS idx_prescription_patient
    ON prescription (patient_id, issued_at DESC);

CREATE TABLE IF NOT EXISTS prescription_item (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL,
    drug_id BIGINT NOT NULL,
    drug_name VARCHAR(128) NOT NULL,
    specification VARCHAR(128),
    dosage VARCHAR(64),
    frequency VARCHAR(64),
    days INTEGER,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    usage_method VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_prescription_item_prescription FOREIGN KEY (prescription_id) REFERENCES prescription (id),
    CONSTRAINT fk_prescription_item_drug FOREIGN KEY (drug_id) REFERENCES drug_info (id)
);

CREATE INDEX IF NOT EXISTS idx_prescription_item_prescription
    ON prescription_item (prescription_id);

CREATE TABLE IF NOT EXISTS payment_order (
    id BIGSERIAL PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    patient_id BIGINT NOT NULL,
    registration_id BIGINT,
    order_type VARCHAR(32) NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    paid_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    pay_status VARCHAR(32) NOT NULL,
    pay_channel VARCHAR(32),
    pay_time TIMESTAMP,
    cashier_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_payment_order_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_payment_order_registration FOREIGN KEY (registration_id) REFERENCES registration (id)
);

CREATE INDEX IF NOT EXISTS idx_payment_order_patient
    ON payment_order (patient_id, created_at DESC);

CREATE TABLE IF NOT EXISTS payment_item (
    id BIGSERIAL PRIMARY KEY,
    payment_order_id BIGINT NOT NULL,
    item_type VARCHAR(32) NOT NULL,
    biz_id BIGINT NOT NULL,
    item_name VARCHAR(128) NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_payment_item_order FOREIGN KEY (payment_order_id) REFERENCES payment_order (id)
);

CREATE INDEX IF NOT EXISTS idx_payment_item_order
    ON payment_item (payment_order_id);

CREATE TABLE IF NOT EXISTS file_record (
    id BIGSERIAL PRIMARY KEY,
    biz_type VARCHAR(64),
    biz_id BIGINT,
    bucket_name VARCHAR(128) NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(64),
    content_type VARCHAR(128),
    file_size BIGINT NOT NULL,
    uploader_id BIGINT,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_file_record_biz
    ON file_record (biz_type, biz_id);

CREATE TABLE IF NOT EXISTS ct_analysis_task (
    id BIGSERIAL PRIMARY KEY,
    ct_image_file_id BIGINT NOT NULL,
    analysis_type VARCHAR(64) NOT NULL,
    task_status VARCHAR(32) NOT NULL,
    input_path VARCHAR(512) NOT NULL,
    failure_reason TEXT,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ct_analysis_task_file FOREIGN KEY (ct_image_file_id) REFERENCES file_record (id)
);

CREATE INDEX IF NOT EXISTS idx_ct_analysis_task_file
    ON ct_analysis_task (ct_image_file_id);

CREATE INDEX IF NOT EXISTS idx_ct_analysis_task_status
    ON ct_analysis_task (task_status);

CREATE TABLE IF NOT EXISTS ct_analysis_result (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL UNIQUE,
    analysis_type VARCHAR(64) NOT NULL,
    predicted_category VARCHAR(32),
    confidence NUMERIC(10, 6),
    probability_b1 NUMERIC(10, 6),
    probability_b2 NUMERIC(10, 6),
    risk_level VARCHAR(32),
    model_name VARCHAR(128),
    doctor_confirm_status VARCHAR(32) NOT NULL DEFAULT 'UNCONFIRMED',
    raw_result_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ct_analysis_result_task FOREIGN KEY (task_id) REFERENCES ct_analysis_task (id)
);
