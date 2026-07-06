-- 兼容两种场景：
-- 1. 全新库初始化
-- 2. 旧版 knowledge_document / knowledge_chunk 已存在时补齐缺失字段
-- 启用 pgvector，用于数据库侧向量距离计算和 TopK 排序
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGSERIAL PRIMARY KEY,
    doc_no VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    file_record_id BIGINT NOT NULL,
    knowledge_type VARCHAR(64) NOT NULL,
    department_id BIGINT,
    tags TEXT,
    audience VARCHAR(32),
    visit_scope VARCHAR(32),
    summary TEXT,
    version_no INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL,
    parser_status VARCHAR(32) NOT NULL,
    chunk_count INTEGER NOT NULL DEFAULT 0,
    token_count INTEGER NOT NULL DEFAULT 0,
    effective_from TIMESTAMP,
    effective_to TIMESTAMP,
    published_at TIMESTAMP,
    offline_at TIMESTAMP,
    last_indexed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS doc_no VARCHAR(64);
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS title VARCHAR(255);
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS file_record_id BIGINT;
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS knowledge_type VARCHAR(64) DEFAULT 'FAQ';
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS department_id BIGINT;
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS tags TEXT;
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS audience VARCHAR(32);
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS visit_scope VARCHAR(32);
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS summary TEXT;
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS version_no INTEGER DEFAULT 1;
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS status VARCHAR(32) DEFAULT 'DRAFT';
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS parser_status VARCHAR(32) DEFAULT 'PENDING';
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS chunk_count INTEGER DEFAULT 0;
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS token_count INTEGER DEFAULT 0;
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS effective_from TIMESTAMP;
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS effective_to TIMESTAMP;
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS published_at TIMESTAMP;
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS offline_at TIMESTAMP;
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS last_indexed_at TIMESTAMP;
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE;

ALTER TABLE knowledge_document ALTER COLUMN version_no SET DEFAULT 1;
ALTER TABLE knowledge_document ALTER COLUMN status SET DEFAULT 'DRAFT';
ALTER TABLE knowledge_document ALTER COLUMN parser_status SET DEFAULT 'PENDING';
ALTER TABLE knowledge_document ALTER COLUMN chunk_count SET DEFAULT 0;
ALTER TABLE knowledge_document ALTER COLUMN token_count SET DEFAULT 0;
ALTER TABLE knowledge_document ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE knowledge_document ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE knowledge_document ALTER COLUMN deleted SET DEFAULT FALSE;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_knowledge_document_file'
    ) THEN
        ALTER TABLE knowledge_document
            ADD CONSTRAINT fk_knowledge_document_file
            FOREIGN KEY (file_record_id) REFERENCES file_record (id);
    END IF;
EXCEPTION
    WHEN undefined_table THEN
        NULL;
    WHEN duplicate_object THEN
        NULL;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_knowledge_document_doc_no
    ON knowledge_document (doc_no);

CREATE INDEX IF NOT EXISTS idx_knowledge_document_status_type_dept
    ON knowledge_document (status, knowledge_type, department_id);

CREATE INDEX IF NOT EXISTS idx_knowledge_document_file
    ON knowledge_document (file_record_id);

CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    chunk_no INTEGER NOT NULL,
    section_path VARCHAR(512),
    section_title VARCHAR(255),
    content_text TEXT NOT NULL,
    content_hash VARCHAR(64),
    char_count INTEGER,
    token_count INTEGER,
    knowledge_type VARCHAR(64) NOT NULL,
    department_id BIGINT,
    tags TEXT,
    document_status VARCHAR(32) NOT NULL,
    source_page VARCHAR(64),
    source_order INTEGER,
    embedding TEXT NOT NULL,
    embedding_model VARCHAR(128),
    embedding_version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE knowledge_chunk ADD COLUMN IF NOT EXISTS document_id BIGINT;
ALTER TABLE knowledge_chunk ADD COLUMN IF NOT EXISTS chunk_no INTEGER;
ALTER TABLE knowledge_chunk ADD COLUMN IF NOT EXISTS section_path VARCHAR(512);
ALTER TABLE knowledge_chunk ADD COLUMN IF NOT EXISTS section_title VARCHAR(255);
ALTER TABLE knowledge_chunk ADD COLUMN IF NOT EXISTS content_text TEXT;
ALTER TABLE knowledge_chunk ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64);
ALTER TABLE knowledge_chunk ADD COLUMN IF NOT EXISTS char_count INTEGER;
ALTER TABLE knowledge_chunk ADD COLUMN IF NOT EXISTS token_count INTEGER;
ALTER TABLE knowledge_chunk ADD COLUMN IF NOT EXISTS knowledge_type VARCHAR(64) DEFAULT 'FAQ';
ALTER TABLE knowledge_chunk ADD COLUMN IF NOT EXISTS department_id BIGINT;
ALTER TABLE knowledge_chunk ADD COLUMN IF NOT EXISTS tags TEXT;
ALTER TABLE knowledge_chunk ADD COLUMN IF NOT EXISTS document_status VARCHAR(32) DEFAULT 'DRAFT';
ALTER TABLE knowledge_chunk ADD COLUMN IF NOT EXISTS source_page VARCHAR(64);
ALTER TABLE knowledge_chunk ADD COLUMN IF NOT EXISTS source_order INTEGER;
ALTER TABLE knowledge_chunk ADD COLUMN IF NOT EXISTS embedding TEXT;
ALTER TABLE knowledge_chunk ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(128);
ALTER TABLE knowledge_chunk ADD COLUMN IF NOT EXISTS embedding_version INTEGER DEFAULT 1;
ALTER TABLE knowledge_chunk ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE knowledge_chunk ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE knowledge_chunk ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE;

ALTER TABLE knowledge_chunk ALTER COLUMN knowledge_type SET DEFAULT 'FAQ';
ALTER TABLE knowledge_chunk ALTER COLUMN document_status SET DEFAULT 'DRAFT';
ALTER TABLE knowledge_chunk ALTER COLUMN embedding_version SET DEFAULT 1;
ALTER TABLE knowledge_chunk ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE knowledge_chunk ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE knowledge_chunk ALTER COLUMN deleted SET DEFAULT FALSE;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_knowledge_chunk_document'
    ) THEN
        ALTER TABLE knowledge_chunk
            ADD CONSTRAINT fk_knowledge_chunk_document
            FOREIGN KEY (document_id) REFERENCES knowledge_document (id);
    END IF;
EXCEPTION
    WHEN undefined_table THEN
        NULL;
    WHEN duplicate_object THEN
        NULL;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_knowledge_chunk_doc_no
    ON knowledge_chunk (document_id, chunk_no);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_filter
    ON knowledge_chunk (document_status, knowledge_type, department_id);

-- 如后续统一 embedding 维度，可按模型/版本增加部分表达式索引，例如：
-- CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_embedding_cosine_v1
--     ON knowledge_chunk USING ivfflat ((CAST(embedding AS vector(1024))) vector_cosine_ops)
--     WITH (lists = 100)
--     WHERE embedding_model = 'text-embedding-v3' AND embedding_version = 1 AND deleted = false AND document_status = 'PUBLISHED';
