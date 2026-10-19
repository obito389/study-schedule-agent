-- Legacy rows intentionally remain NULL: they cannot prove request identity.
ALTER TABLE agent_action ADD COLUMN request_hash VARCHAR(64) NULL;
