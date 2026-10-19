package com.studyplanner.undo;

import jakarta.validation.constraints.NotNull;

/**
 * 撤销请求。
 * <ul>
 *   <li>单步撤销（POST /api/v1/undo/action）：提供 operationKey；</li>
 *   <li>批量撤销（POST /api/v1/undo/batch）：提供 planDraftId，整体回滚该草案的一批动作。</li>
 * </ul>
 * baseRevision 与 apply 一致，走乐观锁：撤销本身是一次写操作，成功后 planning_revision +1。
 */
public class UndoRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "基础版本号(baseRevision)不能为空")
    private Long baseRevision;

    /** 单步撤销：要撤销的原动作 operationKey */
    private String operationKey;

    /** 批量撤销：要整体回滚的来源排程草案 ID */
    private Long planDraftId;

    /** 撤销原因（可选，仅用于前端展示与排障，不入库） */
    private String reason;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBaseRevision() { return baseRevision; }
    public void setBaseRevision(Long baseRevision) { this.baseRevision = baseRevision; }
    public String getOperationKey() { return operationKey; }
    public void setOperationKey(String operationKey) { this.operationKey = operationKey; }
    public Long getPlanDraftId() { return planDraftId; }
    public void setPlanDraftId(Long planDraftId) { this.planDraftId = planDraftId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
