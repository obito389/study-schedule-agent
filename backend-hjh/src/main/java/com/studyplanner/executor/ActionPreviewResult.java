package com.studyplanner.executor;

import java.util.List;

/**
 * 执行前预览结果（不落库、无副作用）。
 * 校验通过后 expectedRevision = currentRevision + 1；若版本不匹配或硬约束不通过，valid=false。
 */
public class ActionPreviewResult {

    private Long userId;
    private Long currentRevision;
    private Long baseRevision;
    /** 乐观锁版本是否匹配（true 才允许 apply） */
    private boolean revisionMatch;
    /** 硬约束是否全部通过 */
    private boolean valid;
    /** 应用后预期的新版本号 */
    private Long expectedRevision;
    /** 硬约束违规信息（空列表表示通过） */
    private List<String> violations;
    /** 每个动作的模拟影响（before/after 预览） */
    private List<ActionItemResult> items;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCurrentRevision() { return currentRevision; }
    public void setCurrentRevision(Long currentRevision) { this.currentRevision = currentRevision; }
    public Long getBaseRevision() { return baseRevision; }
    public void setBaseRevision(Long baseRevision) { this.baseRevision = baseRevision; }
    public boolean isRevisionMatch() { return revisionMatch; }
    public void setRevisionMatch(boolean revisionMatch) { this.revisionMatch = revisionMatch; }
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public Long getExpectedRevision() { return expectedRevision; }
    public void setExpectedRevision(Long expectedRevision) { this.expectedRevision = expectedRevision; }
    public List<String> getViolations() { return violations; }
    public void setViolations(List<String> violations) { this.violations = violations; }
    public List<ActionItemResult> getItems() { return items; }
    public void setItems(List<ActionItemResult> items) { this.items = items; }
}
