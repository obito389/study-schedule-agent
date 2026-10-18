CREATE TABLE user_schedule (
 id VARCHAR(80) PRIMARY KEY,
 revision BIGINT NOT NULL,
 timezone VARCHAR(80) NOT NULL
);
INSERT INTO user_schedule(id, revision, timezone) VALUES ('demo', 0, 'Asia/Shanghai');
CREATE TABLE study_task (
 id VARCHAR(80) PRIMARY KEY, user_id VARCHAR(80) NOT NULL,
 course_id VARCHAR(80), title VARCHAR(200) NOT NULL,
 deadline TIMESTAMP(6) NOT NULL, duration_minutes INT NOT NULL,
 priority INT NOT NULL, status VARCHAR(30) NOT NULL
);
CREATE INDEX idx_task_user ON study_task(user_id);
CREATE TABLE study_course (
 id VARCHAR(80) PRIMARY KEY, user_id VARCHAR(80) NOT NULL, name VARCHAR(200) NOT NULL
);
CREATE TABLE calendar_block (
 id VARCHAR(80) PRIMARY KEY, user_id VARCHAR(80) NOT NULL, owner_id VARCHAR(80) NOT NULL,
 starts_at TIMESTAMP(6) NOT NULL, ends_at TIMESTAMP(6) NOT NULL,
 locked BOOLEAN NOT NULL, kind VARCHAR(30) NOT NULL
);
CREATE INDEX idx_block_user ON calendar_block(user_id);
CREATE TABLE action_execution (
 id VARCHAR(80) PRIMARY KEY, user_id VARCHAR(80) NOT NULL,
 operation_key VARCHAR(160) NOT NULL, request_json LONGTEXT NOT NULL,
 receipt_json LONGTEXT NOT NULL, before_json LONGTEXT NOT NULL, after_json LONGTEXT NOT NULL,
 undone BOOLEAN NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 CONSTRAINT uk_execution_key UNIQUE(user_id, operation_key)
);
CREATE TABLE plan_preview (
 id VARCHAR(80) PRIMARY KEY, user_id VARCHAR(80) NOT NULL, base_revision BIGINT NOT NULL,
 candidate_json LONGTEXT NOT NULL, created_at TIMESTAMP(6) NOT NULL
);
CREATE TABLE profile_fact_record (
 id VARCHAR(80) PRIMARY KEY, user_id VARCHAR(80) NOT NULL,
 fact_json LONGTEXT NOT NULL, fact_version BIGINT NOT NULL,
 CONSTRAINT uk_fact_version UNIQUE(user_id, fact_version)
);
CREATE TABLE import_job (
 id VARCHAR(160) PRIMARY KEY, user_id VARCHAR(80) NOT NULL, job_json LONGTEXT NOT NULL
);
