CREATE TABLE IF NOT EXISTS check_request (
    id BIGSERIAL PRIMARY KEY,
    request_no VARCHAR(64) NOT NULL UNIQUE,
    patient_id BIGINT NOT NULL,
    registration_id BIGINT NOT NULL,
    medical_record_id BIGINT,
    doctor_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    target_department_id BIGINT NOT NULL,
    check_item_code VARCHAR(64) NOT NULL,
    check_item_name VARCHAR(128) NOT NULL,
    clinical_diagnosis VARCHAR(255),
    purpose VARCHAR(255),
    urgent_flag BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL,
    result_summary TEXT,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_check_request_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_check_request_registration FOREIGN KEY (registration_id) REFERENCES registration (id),
    CONSTRAINT fk_check_request_medical_record FOREIGN KEY (medical_record_id) REFERENCES medical_record (id),
    CONSTRAINT fk_check_request_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id),
    CONSTRAINT fk_check_request_department FOREIGN KEY (department_id) REFERENCES department (id),
    CONSTRAINT fk_check_request_target_department FOREIGN KEY (target_department_id) REFERENCES department (id)
);

CREATE INDEX IF NOT EXISTS idx_check_request_patient
    ON check_request (patient_id, requested_at DESC);

CREATE INDEX IF NOT EXISTS idx_check_request_status
    ON check_request (status, requested_at DESC);

CREATE TABLE IF NOT EXISTS inspection_request (
    id BIGSERIAL PRIMARY KEY,
    request_no VARCHAR(64) NOT NULL UNIQUE,
    patient_id BIGINT NOT NULL,
    registration_id BIGINT NOT NULL,
    medical_record_id BIGINT,
    doctor_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    target_department_id BIGINT NOT NULL,
    inspection_item_code VARCHAR(64) NOT NULL,
    inspection_item_name VARCHAR(128) NOT NULL,
    sample_type VARCHAR(64),
    urgent_flag BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL,
    result_summary TEXT,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_inspection_request_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_inspection_request_registration FOREIGN KEY (registration_id) REFERENCES registration (id),
    CONSTRAINT fk_inspection_request_medical_record FOREIGN KEY (medical_record_id) REFERENCES medical_record (id),
    CONSTRAINT fk_inspection_request_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id),
    CONSTRAINT fk_inspection_request_department FOREIGN KEY (department_id) REFERENCES department (id),
    CONSTRAINT fk_inspection_request_target_department FOREIGN KEY (target_department_id) REFERENCES department (id)
);

CREATE INDEX IF NOT EXISTS idx_inspection_request_patient
    ON inspection_request (patient_id, requested_at DESC);

CREATE INDEX IF NOT EXISTS idx_inspection_request_status
    ON inspection_request (status, requested_at DESC);

CREATE TABLE IF NOT EXISTS disposal_request (
    id BIGSERIAL PRIMARY KEY,
    request_no VARCHAR(64) NOT NULL UNIQUE,
    patient_id BIGINT NOT NULL,
    registration_id BIGINT NOT NULL,
    medical_record_id BIGINT,
    doctor_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    disposal_item_code VARCHAR(64) NOT NULL,
    disposal_item_name VARCHAR(128) NOT NULL,
    quantity INTEGER,
    remark VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_disposal_request_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_disposal_request_registration FOREIGN KEY (registration_id) REFERENCES registration (id),
    CONSTRAINT fk_disposal_request_medical_record FOREIGN KEY (medical_record_id) REFERENCES medical_record (id),
    CONSTRAINT fk_disposal_request_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id),
    CONSTRAINT fk_disposal_request_department FOREIGN KEY (department_id) REFERENCES department (id)
);

CREATE INDEX IF NOT EXISTS idx_disposal_request_patient
    ON disposal_request (patient_id, created_at DESC);

CREATE TABLE IF NOT EXISTS check_result (
    id BIGSERIAL PRIMARY KEY,
    check_request_id BIGINT NOT NULL,
    report_no VARCHAR(64) NOT NULL UNIQUE,
    result_text TEXT,
    result_summary TEXT,
    conclusion TEXT,
    report_file_id BIGINT,
    report_doctor_id BIGINT,
    reported_at TIMESTAMP,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_check_result_request FOREIGN KEY (check_request_id) REFERENCES check_request (id),
    CONSTRAINT fk_check_result_file FOREIGN KEY (report_file_id) REFERENCES file_record (id),
    CONSTRAINT fk_check_result_doctor FOREIGN KEY (report_doctor_id) REFERENCES doctor (id)
);

CREATE INDEX IF NOT EXISTS idx_check_result_request
    ON check_result (check_request_id);

CREATE TABLE IF NOT EXISTS inspection_result (
    id BIGSERIAL PRIMARY KEY,
    inspection_request_id BIGINT NOT NULL,
    report_no VARCHAR(64) NOT NULL UNIQUE,
    summary TEXT,
    conclusion TEXT,
    report_file_id BIGINT,
    report_doctor_id BIGINT,
    reported_at TIMESTAMP,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_inspection_result_request FOREIGN KEY (inspection_request_id) REFERENCES inspection_request (id),
    CONSTRAINT fk_inspection_result_file FOREIGN KEY (report_file_id) REFERENCES file_record (id),
    CONSTRAINT fk_inspection_result_doctor FOREIGN KEY (report_doctor_id) REFERENCES doctor (id)
);

CREATE INDEX IF NOT EXISTS idx_inspection_result_request
    ON inspection_result (inspection_request_id);

CREATE TABLE IF NOT EXISTS inspection_result_item (
    id BIGSERIAL PRIMARY KEY,
    inspection_result_id BIGINT NOT NULL,
    item_code VARCHAR(64) NOT NULL,
    item_name VARCHAR(128) NOT NULL,
    result_value VARCHAR(64) NOT NULL,
    unit VARCHAR(32),
    reference_range VARCHAR(64),
    abnormal_flag VARCHAR(16),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_inspection_result_item_result FOREIGN KEY (inspection_result_id) REFERENCES inspection_result (id)
);

CREATE INDEX IF NOT EXISTS idx_inspection_result_item_result
    ON inspection_result_item (inspection_result_id);

CREATE TABLE IF NOT EXISTS drug_dispense_record (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    dispense_type VARCHAR(32) NOT NULL,
    quantity INTEGER,
    status VARCHAR(32) NOT NULL,
    operate_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_drug_dispense_record_prescription FOREIGN KEY (prescription_id) REFERENCES prescription (id),
    CONSTRAINT fk_drug_dispense_record_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_drug_dispense_record_operator FOREIGN KEY (operator_id) REFERENCES sys_user (id)
);

CREATE INDEX IF NOT EXISTS idx_drug_dispense_record_prescription
    ON drug_dispense_record (prescription_id);

CREATE TABLE IF NOT EXISTS refund_record (
    id BIGSERIAL PRIMARY KEY,
    payment_order_id BIGINT NOT NULL,
    refund_no VARCHAR(64) NOT NULL UNIQUE,
    refund_amount NUMERIC(10, 2) NOT NULL,
    refund_reason VARCHAR(255),
    operator_id BIGINT,
    refund_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_refund_record_order FOREIGN KEY (payment_order_id) REFERENCES payment_order (id),
    CONSTRAINT fk_refund_record_operator FOREIGN KEY (operator_id) REFERENCES sys_user (id)
);

CREATE INDEX IF NOT EXISTS idx_refund_record_order
    ON refund_record (payment_order_id);
