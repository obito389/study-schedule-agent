-- ============================================================
-- 学习日程 Agent · 初始库表结构（16 张核心表）
-- 对应课设报告 2.4 节，以后作为 Flyway 迁移脚本 V1__init__schema.sql
-- 约定：引擎 InnoDB / 字符集 utf8mb4 / 主键 id BIGINT 自增
--       业务表必须带 user_id 归属 / 时间戳 DATETIME(3) / 表名字段名 snake_case
-- ============================================================

CREATE DATABASE IF NOT EXISTS studyplanner
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE studyplanner;

-- ============================================================
-- 1. app_user（账户根表，所有业务表的归属）
-- ============================================================
CREATE TABLE app_user (
  id                 BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username           VARCHAR(64)  NOT NULL,
  password_hash      VARCHAR(255) NOT NULL,
  timezone           VARCHAR(64)  NOT NULL DEFAULT 'Asia/Shanghai',
  planning_revision  BIGINT       NOT NULL DEFAULT 0,
  profile_version    BIGINT       NOT NULL DEFAULT 0,
  learning_enabled   TINYINT(1)   NOT NULL DEFAULT 1,
  created_at         DATETIME(3)  NOT NULL,
  CONSTRAINT uk_appuser_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 2. semester（学期）
-- ============================================================
CREATE TABLE semester (
  id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT      NOT NULL,
  name        VARCHAR(64) NOT NULL,
  start_date  DATE        NOT NULL COMMENT '第一教学周周一',
  week_count  INT         NOT NULL COMMENT '1~30 周',
  CONSTRAINT fk_semester_user FOREIGN KEY (user_id) REFERENCES app_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 3. course（课程）
-- ============================================================
CREATE TABLE course (
  id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  semester_id  BIGINT       NOT NULL,
  title        VARCHAR(100) NOT NULL,
  teacher      VARCHAR(64)  NULL,
  location     VARCHAR(100) NULL,
  CONSTRAINT fk_course_semester FOREIGN KEY (semester_id) REFERENCES semester (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 4. course_session（课程节次：单双周、星期、时间）
-- ============================================================
CREATE TABLE course_session (
  id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  course_id   BIGINT      NOT NULL,
  weekday     TINYINT     NOT NULL COMMENT '周一=1',
  start_time  TIME        NOT NULL,
  end_time    TIME        NOT NULL,
  start_week  INT         NOT NULL,
  end_week    INT         NOT NULL,
  week_type   VARCHAR(8)  NOT NULL DEFAULT 'ALL' COMMENT 'ALL/ODD/EVEN',
  CONSTRAINT fk_coursesession_course FOREIGN KEY (course_id) REFERENCES course (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 5. user_preference（用户偏好设置：每日上限、时间槽粒度、规划窗口）
-- ============================================================
CREATE TABLE user_preference (
  user_id           BIGINT      NOT NULL,
  daily_limit_min   INT         NOT NULL DEFAULT 120,
  block_min         INT         NOT NULL DEFAULT 30,
  horizon_days      INT         NOT NULL DEFAULT 14,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_userpref_user FOREIGN KEY (user_id) REFERENCES app_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 6. availability_window（可用/禁止时段）
-- ============================================================
CREATE TABLE availability_window (
  id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT      NOT NULL,
  kind        VARCHAR(16) NOT NULL COMMENT 'WEEKLY_ALLOW / DATE_BLOCK',
  weekday     TINYINT     NULL COMMENT 'WEEKLY_ALLOW 用，1=周一',
  local_date  DATE        NULL COMMENT 'DATE_BLOCK 用，指定日期',
  start_time  TIME        NOT NULL,
  end_time    TIME        NOT NULL,
  CONSTRAINT fk_availwin_user FOREIGN KEY (user_id) REFERENCES app_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 7. import_job（多模态导入任务：截图/语音/文字）
-- ============================================================
CREATE TABLE import_job (
  id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id         BIGINT       NOT NULL,
  request_key     VARCHAR(64)  NOT NULL,
  modality        VARCHAR(12)  NOT NULL COMMENT 'TEXT/IMAGE/AUDIO',
  state           VARCHAR(20)  NOT NULL DEFAULT 'UPLOADED'
                  COMMENT 'UPLOADED/PROCESSING/REVIEW/COMMITTED/FAILED/EXPIRED',
  source_ref      VARCHAR(255) NULL,
  source_hash     CHAR(64)     NULL,
  proposal_json   JSON         NULL,
  transcript      TEXT         NULL,
  base_revision   BIGINT       NOT NULL DEFAULT 0,
  version         BIGINT       NOT NULL DEFAULT 0,
  lease_until     DATETIME(3)  NULL,
  error_code      VARCHAR(32)  NULL,
  created_at      DATETIME(3)  NOT NULL,
  expires_at      DATETIME(3)  NULL,
  CONSTRAINT fk_importjob_user FOREIGN KEY (user_id) REFERENCES app_user (id),
  CONSTRAINT uk_importjob_user_requestkey UNIQUE (user_id, request_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 8. task（任务）
-- ============================================================
CREATE TABLE task (
  id                   BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id              BIGINT       NOT NULL,
  course_id            BIGINT       NULL,
  import_job_id        BIGINT       NULL,
  title                VARCHAR(150) NOT NULL,
  description          TEXT         NULL,
  deadline             DATETIME(3)  NULL,
  remaining_min        INT          NOT NULL DEFAULT 0,
  priority             TINYINT      NOT NULL DEFAULT 2 COMMENT '1~3，3最高',
  status               VARCHAR(16)  NOT NULL DEFAULT 'TODO'
                       COMMENT 'TODO/DOING/DONE/CANCELLED',
  task_type            VARCHAR(32)  NULL,
  initial_estimate_min INT          NULL,
  estimate_source      VARCHAR(16)  NULL,
  updated_at           DATETIME(3)  NOT NULL,
  CONSTRAINT fk_task_user     FOREIGN KEY (user_id)       REFERENCES app_user (id),
  CONSTRAINT fk_task_course   FOREIGN KEY (course_id)     REFERENCES course (id),
  CONSTRAINT fk_task_importjob FOREIGN KEY (import_job_id) REFERENCES import_job (id),
  INDEX idx_task_user_status_deadline (user_id, status, deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 9. task_step（任务步骤）
-- ============================================================
CREATE TABLE task_step (
  id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  task_id      BIGINT       NOT NULL,
  order_no     INT          NOT NULL,
  title        VARCHAR(150) NOT NULL,
  remaining_min INT         NOT NULL DEFAULT 0,
  status       VARCHAR(16)  NOT NULL DEFAULT 'TODO',
  CONSTRAINT fk_taskstep_task FOREIGN KEY (task_id) REFERENCES task (id),
  CONSTRAINT uk_taskstep_task_order UNIQUE (task_id, order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 10. plan_draft（排程草案：候选方案，不直接写日历）
-- ============================================================
CREATE TABLE plan_draft (
  id                  BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id             BIGINT      NOT NULL,
  base_revision       BIGINT      NOT NULL,
  base_profile_version BIGINT     NOT NULL DEFAULT 0,
  status              VARCHAR(16) NOT NULL DEFAULT 'DRAFT'
                      COMMENT 'DRAFT/APPLIED/EXPIRED',
  range_start         DATETIME(3) NOT NULL,
  range_end           DATETIME(3) NOT NULL,
  proposal_json       JSON        NULL,
  request_key         VARCHAR(64) NOT NULL,
  created_at          DATETIME(3) NOT NULL,
  applied_at          DATETIME(3) NULL,
  CONSTRAINT fk_plandraft_user FOREIGN KEY (user_id) REFERENCES app_user (id),
  CONSTRAINT uk_plandraft_user_requestkey UNIQUE (user_id, request_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 11. time_block（正式时间块：日历上的实际安排）
-- ============================================================
CREATE TABLE time_block (
  id              BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id         BIGINT      NOT NULL,
  task_id         BIGINT      NOT NULL,
  step_id         BIGINT      NULL,
  source_draft_id BIGINT      NULL,
  start_at        DATETIME(3) NOT NULL,
  end_at          DATETIME(3) NOT NULL,
  status          VARCHAR(16) NOT NULL DEFAULT 'PLANNED'
                  COMMENT 'PLANNED/DONE/MISSED/CANCELLED',
  locked          TINYINT(1)  NOT NULL DEFAULT 0,
  CONSTRAINT fk_timeblock_user   FOREIGN KEY (user_id)         REFERENCES app_user (id),
  CONSTRAINT fk_timeblock_task  FOREIGN KEY (task_id)         REFERENCES task (id),
  CONSTRAINT fk_timeblock_step   FOREIGN KEY (step_id)         REFERENCES task_step (id),
  CONSTRAINT fk_timeblock_draft  FOREIGN KEY (source_draft_id)  REFERENCES plan_draft (id),
  INDEX idx_timeblock_user_start (user_id, start_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 12. execution_log（耗时反馈日志）
-- ============================================================
CREATE TABLE execution_log (
  id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id        BIGINT       NOT NULL,
  task_id        BIGINT       NOT NULL,
  block_id       BIGINT       NULL,
  step_id        BIGINT       NULL,
  actual_min     INT          NOT NULL DEFAULT 0,
  remaining_after INT         NOT NULL DEFAULT 0,
  note           VARCHAR(500) NULL,
  feedback_key   VARCHAR(64)  NOT NULL,
  created_at     DATETIME(3)  NOT NULL,
  CONSTRAINT fk_execlog_user  FOREIGN KEY (user_id)   REFERENCES app_user (id),
  CONSTRAINT fk_execlog_task  FOREIGN KEY (task_id)   REFERENCES task (id),
  CONSTRAINT fk_execlog_block FOREIGN KEY (block_id)   REFERENCES time_block (id),
  CONSTRAINT uk_execlog_user_feedbackkey UNIQUE (user_id, feedback_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 13. profile_fact（用户画像事实：偏好、例外、候选）
-- ============================================================
CREATE TABLE profile_fact (
  id           BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id      BIGINT        NOT NULL,
  `key`        VARCHAR(64)   NOT NULL,
  value_json   JSON          NULL,
  source       VARCHAR(16)   NOT NULL COMMENT 'EXPLICIT/INFERRED',
  scope_json   JSON          NULL,
  state        VARCHAR(16)   NOT NULL DEFAULT 'CANDIDATE'
               COMMENT 'CANDIDATE/ACTIVE/REJECTED/DELETED',
  confidence   DECIMAL(4,3)  NOT NULL DEFAULT 0,
  evidence_ids JSON          NULL,
  version      BIGINT        NOT NULL DEFAULT 0,
  expires_at   DATETIME(3)   NULL,
  updated_at   DATETIME(3)   NOT NULL,
  CONSTRAINT fk_profilefact_user FOREIGN KEY (user_id) REFERENCES app_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 14. profile_event（画像事件：反馈、调整、否认的审计事件）
-- ============================================================
CREATE TABLE profile_event (
  id           BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id      BIGINT      NOT NULL,
  event_key    VARCHAR(64) NOT NULL,
  type         VARCHAR(32) NOT NULL,
  payload_json JSON        NULL,
  created_at    DATETIME(3) NOT NULL,
  CONSTRAINT fk_profileevent_user FOREIGN KEY (user_id) REFERENCES app_user (id),
  CONSTRAINT uk_profileevent_user_eventkey UNIQUE (user_id, event_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 15. learning_stat（同类任务耗时统计：平滑倍率）
-- ============================================================
CREATE TABLE learning_stat (
  id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id       BIGINT       NOT NULL,
  task_type     VARCHAR(32)  NOT NULL,
  sample_count  INT          NOT NULL DEFAULT 0,
  ratio         DECIMAL(5,3) NOT NULL DEFAULT 1.000,
  last_event_id BIGINT       NULL,
  updated_at    DATETIME(3)  NOT NULL,
  CONSTRAINT fk_learningstat_user FOREIGN KEY (user_id) REFERENCES app_user (id),
  CONSTRAINT uk_learningstat_user_tasktype UNIQUE (user_id, task_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 16. agent_action（Agent 动作日志：授权、前后快照、撤销依据）
-- ============================================================
CREATE TABLE agent_action (
  id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id             BIGINT       NOT NULL,
  operation_key       VARCHAR(64)  NOT NULL,
  type                VARCHAR(24)  NOT NULL,
  status              VARCHAR(20)  NOT NULL DEFAULT 'PREVIEW'
                      COMMENT 'PREVIEW/APPLIED/FAILED/UNDONE',
  base_revision       BIGINT       NOT NULL DEFAULT 0,
  applied_revision    BIGINT       NOT NULL DEFAULT 0,
  base_profile_version BIGINT      NOT NULL DEFAULT 0,
  authorization_json  JSON         NULL,
  before_json         JSON         NULL,
  after_json          JSON         NULL,
  error_code          VARCHAR(32)  NULL,
  created_at          DATETIME(3)  NOT NULL,
  CONSTRAINT fk_agentaction_user FOREIGN KEY (user_id) REFERENCES app_user (id),
  CONSTRAINT uk_agentaction_user_opkey UNIQUE (user_id, operation_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 验证：应看到 16 张表
-- SHOW TABLES;
-- ============================================================
