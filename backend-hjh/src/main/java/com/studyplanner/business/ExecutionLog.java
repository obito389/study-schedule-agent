package com.studyplanner.business;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * 执行日志实体（execution_log 表）
 */
@Entity
@Table(name = "execution_log")
public class ExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Column(name = "task_id", nullable = false)
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @Column(name = "block_id")
    private Long blockId;

    @Column(name = "step_id")
    private Long stepId;

    @Column(name = "actual_min", nullable = false)
    @NotNull(message = "实际用时不能为空")
    @Min(value = 0, message = "实际用时不能为负数")
    private Integer actualMin = 0;

    @Column(name = "remaining_after", nullable = false)
    @NotNull(message = "剩余时间不能为空")
    @Min(value = 0, message = "剩余时间不能为负数")
    private Integer remainingAfter = 0;

    @Column(length = 500)
    @Size(max = 500, message = "备注不能超过500个字符")
    private String note;

    @Column(name = "feedback_key", nullable = false, length = 64)
    @NotBlank(message = "反馈key不能为空")
    @Size(max = 64, message = "反馈key不能超过64个字符")
    private String feedbackKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public ExecutionLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getBlockId() { return blockId; }
    public void setBlockId(Long blockId) { this.blockId = blockId; }
    public Long getStepId() { return stepId; }
    public void setStepId(Long stepId) { this.stepId = stepId; }
    public Integer getActualMin() { return actualMin; }
    public void setActualMin(Integer actualMin) { this.actualMin = actualMin; }
    public Integer getRemainingAfter() { return remainingAfter; }
    public void setRemainingAfter(Integer remainingAfter) { this.remainingAfter = remainingAfter; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getFeedbackKey() { return feedbackKey; }
    public void setFeedbackKey(String feedbackKey) { this.feedbackKey = feedbackKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
