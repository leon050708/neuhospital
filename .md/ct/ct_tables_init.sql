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
