package com.studyplanner.business;

import com.studyplanner.enums.BlockStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * 时间块实体（time_block 表）
 */
@Entity
@Table(name = "time_block")
public class TimeBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Column(name = "task_id", nullable = false)
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @Column(name = "step_id")
    private Long stepId;

    @Column(name = "source_draft_id")
    private Long sourceDraftId;

    @Column(name = "start_at", nullable = false)
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @NotNull(message = "状态不能为空")
    private BlockStatus status = BlockStatus.PLANNED;

    @Column(nullable = false)
    @NotNull(message = "锁定状态不能为空")
    private Boolean locked = false;

    public TimeBlock() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getStepId() { return stepId; }
    public void setStepId(Long stepId) { this.stepId = stepId; }
    public Long getSourceDraftId() { return sourceDraftId; }
    public void setSourceDraftId(Long sourceDraftId) { this.sourceDraftId = sourceDraftId; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public BlockStatus getStatus() { return status; }
    public void setStatus(BlockStatus status) { this.status = status; }
    public Boolean getLocked() { return locked; }
    public void setLocked(Boolean locked) { this.locked = locked; }
}
