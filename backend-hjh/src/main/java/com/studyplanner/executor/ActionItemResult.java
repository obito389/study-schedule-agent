package com.studyplanner.executor;

import com.studyplanner.enums.ActionType;

/**
 * 单个动作的执行结果。
 * status 取值：PREVIEW（仅预览）/ APPLIED（已应用）/ IDEMPOTENT（幂等命中）/ FAILED（失败）。
 */
public class ActionItemResult {

    private String operationKey;
    private ActionType type;
    private String status;
    /** 影响的实体主键（CREATE 为新生成 ID，UPDATE/DELETE 为目标 ID） */
    private Long targetId;
    /** 实体类型：TASK / STEP / BLOCK */
    private String entityType;
    private String beforeJson;
    private String afterJson;
    private String errorCode;
    private String message;

    public static ActionItemResult preview(ActionCommand cmd, Long targetId, String entityType,
                                           String beforeJson, String afterJson) {
        ActionItemResult r = new ActionItemResult();
        r.operationKey = cmd.getOperationKey();
        r.type = cmd.getType();
        r.status = "PREVIEW";
        r.targetId = targetId;
        r.entityType = entityType;
        r.beforeJson = beforeJson;
        r.afterJson = afterJson;
        return r;
    }

    public static ActionItemResult applied(ActionCommand cmd, Long targetId, String entityType,
                                           String beforeJson, String afterJson) {
        ActionItemResult r = new ActionItemResult();
        r.operationKey = cmd.getOperationKey();
        r.type = cmd.getType();
        r.status = "APPLIED";
        r.targetId = targetId;
        r.entityType = entityType;
        r.beforeJson = beforeJson;
        r.afterJson = afterJson;
        r.message = "执行成功";
        return r;
    }

    public static ActionItemResult idempotent(ActionCommand cmd, Long targetId, String entityType,
                                              String beforeJson, String afterJson) {
        ActionItemResult r = new ActionItemResult();
        r.operationKey = cmd.getOperationKey();
        r.type = cmd.getType();
        r.status = "IDEMPOTENT";
        r.targetId = targetId;
        r.entityType = entityType;
        r.beforeJson = beforeJson;
        r.afterJson = afterJson;
        r.message = "重复提交，已返回首次执行结果";
        return r;
    }

    /** 撤销结果：原动作用 operationKey + type 标识（撤销时手上是 AgentAction 快照，不是 ActionCommand） */
    public static ActionItemResult undone(String operationKey, ActionType type, Long targetId,
                                          String entityType, String beforeJson, String afterJson) {
        ActionItemResult r = new ActionItemResult();
        r.operationKey = operationKey;
        r.type = type;
        r.status = "UNDONE";
        r.targetId = targetId;
        r.entityType = entityType;
        r.beforeJson = beforeJson;
        r.afterJson = afterJson;
        r.message = "已撤销";
        return r;
    }

    public String getOperationKey() { return operationKey; }
    public void setOperationKey(String operationKey) { this.operationKey = operationKey; }
    public ActionType getType() { return type; }
    public void setType(ActionType type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getBeforeJson() { return beforeJson; }
    public void setBeforeJson(String beforeJson) { this.beforeJson = beforeJson; }
    public String getAfterJson() { return afterJson; }
    public void setAfterJson(String afterJson) { this.afterJson = afterJson; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
