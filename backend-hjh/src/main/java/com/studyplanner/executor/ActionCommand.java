package com.studyplanner.executor;

import com.studyplanner.enums.ActionType;
import com.studyplanner.enums.BlockStatus;
import com.studyplanner.enums.StepStatus;
import com.studyplanner.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Agent 生成的单个动作命令（Action 列表中的一项）。
 * 采用平铺字段，按 {@link ActionType} 取用对应字段：
 * <ul>
 *   <li>*_TASK 用任务字段（title / deadline / remainingMin ...）</li>
 *   <li>*_STEP 用步骤字段（taskId / orderNo / stepTitle ...）</li>
 *   <li>*_BLOCK 用时间块字段（taskId / startAt / endAt / blockStatus ...）</li>
 * </ul>
 * UPDATE_* / DELETE_* 通过 targetId 指定目标；CREATE_* 时 targetId 为空。
 */
public class ActionCommand {

    /** 幂等键：同一用户下唯一，重复提交据此去重 */
    @NotBlank(message = "操作key(operationKey)不能为空")
    private String operationKey;

    /** 动作类型（9 种） */
    @NotNull(message = "动作类型(type)不能为空")
    private ActionType type;

    /** 更新/删除目标的主键，CREATE 时为空 */
    private Long targetId;

    // ---------------- 任务（Task）字段 ----------------
    private String title;
    private String description;
    private LocalDateTime deadline;
    private Integer remainingMin;
    private Integer priority;
    private TaskStatus status;
    private String taskType;
    private Long courseId;
    private Integer initialEstimateMin;

    // ---------------- 步骤（TaskStep）字段 ----------------
    /** 步骤所属任务 ID */
    private Long taskId;
    private Integer orderNo;
    private String stepTitle;
    private Integer stepRemainingMin;
    private StepStatus stepStatus;

    // ---------------- 时间块（TimeBlock）字段 ----------------
    private Long stepId;
    private Long sourceDraftId;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private BlockStatus blockStatus;
    private Boolean locked;

    public String getOperationKey() { return operationKey; }
    public void setOperationKey(String operationKey) { this.operationKey = operationKey; }
    public ActionType getType() { return type; }
    public void setType(ActionType type) { this.type = type; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public Integer getRemainingMin() { return remainingMin; }
    public void setRemainingMin(Integer remainingMin) { this.remainingMin = remainingMin; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Integer getInitialEstimateMin() { return initialEstimateMin; }
    public void setInitialEstimateMin(Integer initialEstimateMin) { this.initialEstimateMin = initialEstimateMin; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Integer getOrderNo() { return orderNo; }
    public void setOrderNo(Integer orderNo) { this.orderNo = orderNo; }
    public String getStepTitle() { return stepTitle; }
    public void setStepTitle(String stepTitle) { this.stepTitle = stepTitle; }
    public Integer getStepRemainingMin() { return stepRemainingMin; }
    public void setStepRemainingMin(Integer stepRemainingMin) { this.stepRemainingMin = stepRemainingMin; }
    public StepStatus getStepStatus() { return stepStatus; }
    public void setStepStatus(StepStatus stepStatus) { this.stepStatus = stepStatus; }

    public Long getStepId() { return stepId; }
    public void setStepId(Long stepId) { this.stepId = stepId; }
    public Long getSourceDraftId() { return sourceDraftId; }
    public void setSourceDraftId(Long sourceDraftId) { this.sourceDraftId = sourceDraftId; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public BlockStatus getBlockStatus() { return blockStatus; }
    public void setBlockStatus(BlockStatus blockStatus) { this.blockStatus = blockStatus; }
    public Boolean getLocked() { return locked; }
    public void setLocked(Boolean locked) { this.locked = locked; }
}
