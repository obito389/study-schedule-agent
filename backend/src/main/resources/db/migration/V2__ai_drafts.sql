CREATE TABLE ai_draft (
 id VARCHAR(80) PRIMARY KEY,
 user_id VARCHAR(80) NOT NULL,
 base_revision BIGINT NOT NULL,
 preview_json LONGTEXT NOT NULL,
 command_json LONGTEXT,
 created_at TIMESTAMP(6) NOT NULL
);
