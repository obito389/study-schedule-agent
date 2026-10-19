ALTER TABLE profile_fact ADD COLUMN contract_json LONGTEXT;
ALTER TABLE import_job ADD COLUMN contract_json LONGTEXT;
INSERT INTO app_user(id,username,password_hash,timezone,planning_revision,profile_version,learning_enabled,created_at)
VALUES(1,'team-demo','LOCAL_DEMO_NO_PASSWORD_LOGIN','Asia/Shanghai',0,1,TRUE,CURRENT_TIMESTAMP);
INSERT INTO profile_fact(user_id,`key`,value_json,source,scope_json,state,confidence,evidence_ids,version,updated_at)
VALUES(1,'planning.dailyLimitMinutes','240','MANUAL','"GLOBAL"','ACTIVE',1.000,'["team-bootstrap"]',1,CURRENT_TIMESTAMP);
