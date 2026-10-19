-- ============================================================
-- 第 4 周：版本与撤销能力的数据支撑
--   1) agent_action 增加 plan_draft_id，作为「按草案批量撤销」的分组锚点
--   2) 为「按草案查动作」建立索引，加速批量撤销定位
-- 说明：
--   * status 列已是 VARCHAR(20)，可容纳新增状态 UNDONE（6 字符），无需改列；
--   * 存量记录 plan_draft_id 为 NULL（当时未落库），批量撤销只对新增记录生效。
-- ============================================================

ALTER TABLE agent_action
    ADD COLUMN plan_draft_id BIGINT NULL COMMENT '来源排程草案ID，用于批量撤销分组' AFTER applied_revision;

ALTER TABLE agent_action
    ADD CONSTRAINT fk_agentaction_plan_draft
        FOREIGN KEY (plan_draft_id) REFERENCES plan_draft (id);

ALTER TABLE agent_action
    ADD INDEX idx_agentaction_user_draft_status (user_id, plan_draft_id, status);
