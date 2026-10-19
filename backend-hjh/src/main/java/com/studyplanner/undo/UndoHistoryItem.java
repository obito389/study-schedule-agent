package com.studyplanner.undo;

import com.studyplanner.business.AgentAction;
import com.studyplanner.enums.ActionStatus;
import com.studyplanner.enums.ActionType;

import java.time.LocalDateTime;

/**
 * 历史版本条目：把 agent_action 的 before_json / after_json 快照以「版本链」形式暴露给前端。
 * 每条记录即一次变更的「前一状态 → 后一状态」，按 appliedRevision 可还原某版本的规划内容。
 */
public class UndoHistoryItem {

    private Long id;
    private String operationKey;
    private ActionType type;
    private ActionStatus status;
    private Long baseRevision;
    private Long appliedRevision;
    private Long planDraftId;
    private String errorCode;
    private String beforeJson;
    private String afterJson;
    private LocalDateTime createdAt;

    public static UndoHistoryItem from(AgentAction a) {
        UndoHistoryItem i = new UndoHistoryItem();
        i.id = a.getId();
        i.operationKey = a.getOperationKey();
        i.type = a.getType();
        i.status = a.getStatus();
        i.baseRevision = a.getBaseRevision();
        i.appliedRevision = a.getAppliedRevision();
        i.planDraftId = a.getPlanDraftId();
        i.errorCode = a.getErrorCode();
        i.beforeJson = a.getBeforeJson();
        i.afterJson = a.getAfterJson();
        i.createdAt = a.getCreatedAt();
        return i;
    }

    public Long getId() { return id; }
    public String getOperationKey() { return operationKey; }
    public ActionType getType() { return type; }
    public ActionStatus getStatus() { return status; }
    public Long getBaseRevision() { return baseRevision; }
    public Long getAppliedRevision() { return appliedRevision; }
    public Long getPlanDraftId() { return planDraftId; }
    public String getErrorCode() { return errorCode; }
    public String getBeforeJson() { return beforeJson; }
    public String getAfterJson() { return afterJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
