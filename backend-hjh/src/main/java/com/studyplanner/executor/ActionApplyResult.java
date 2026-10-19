package com.studyplanner.executor;

import java.util.List;

/**
 * 执行后确认（apply）结果。
 * 成功时 newRevision 为推进后的 planning_revision；幂等命中时 idempotent=true 且不推进版本。
 */
public class ActionApplyResult {

    private Long userId;
    private Long baseRevision;
    private Long newRevision;
    /** 是否为幂等重复提交（全部动作此前已应用） */
    private boolean idempotent;
    /** 本次实际应用的动作数量 */
    private int appliedCount;
    private List<ActionItemResult> items;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBaseRevision() { return baseRevision; }
    public void setBaseRevision(Long baseRevision) { this.baseRevision = baseRevision; }
    public Long getNewRevision() { return newRevision; }
    public void setNewRevision(Long newRevision) { this.newRevision = newRevision; }
    public boolean isIdempotent() { return idempotent; }
    public void setIdempotent(boolean idempotent) { this.idempotent = idempotent; }
    public int getAppliedCount() { return appliedCount; }
    public void setAppliedCount(int appliedCount) { this.appliedCount = appliedCount; }
    public List<ActionItemResult> getItems() { return items; }
    public void setItems(List<ActionItemResult> items) { this.items = items; }
}
