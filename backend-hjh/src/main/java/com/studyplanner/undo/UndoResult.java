package com.studyplanner.undo;

import com.studyplanner.executor.ActionItemResult;

import java.util.List;

/**
 * 撤销结果。
 * 撤销成功后 planning_revision 推进到 newRevision（撤销本身是一次写操作）。
 */
public class UndoResult {

    private Long userId;
    private Long baseRevision;
    private Long newRevision;
    /** 本次实际撤销的动作数量 */
    private int undoneCount;
    private List<ActionItemResult> items;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBaseRevision() { return baseRevision; }
    public void setBaseRevision(Long baseRevision) { this.baseRevision = baseRevision; }
    public Long getNewRevision() { return newRevision; }
    public void setNewRevision(Long newRevision) { this.newRevision = newRevision; }
    public int getUndoneCount() { return undoneCount; }
    public void setUndoneCount(int undoneCount) { this.undoneCount = undoneCount; }
    public List<ActionItemResult> getItems() { return items; }
    public void setItems(List<ActionItemResult> items) { this.items = items; }
}
