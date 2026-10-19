package com.studyplanner.business;

import com.studyplanner.enums.StepStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * 任务步骤实体（task_step 表）
 */
@Entity
@Table(name = "task_step")
public class TaskStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @Column(name = "order_no", nullable = false)
    @NotNull(message = "排序号不能为空")
    @Min(value = 1, message = "排序号最小为1")
    private Integer orderNo;

    @Column(nullable = false, length = 150)
    @NotBlank(message = "步骤标题不能为空")
    @Size(max = 150, message = "步骤标题不能超过150个字符")
    private String title;

    @Column(name = "remaining_min", nullable = false)
    @NotNull(message = "剩余时间不能为空")
    @Min(value = 0, message = "剩余时间不能为负数")
    private Integer remainingMin = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @NotNull(message = "状态不能为空")
    private StepStatus status = StepStatus.TODO;

    public TaskStep() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Integer getOrderNo() { return orderNo; }
    public void setOrderNo(Integer orderNo) { this.orderNo = orderNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getRemainingMin() { return remainingMin; }
    public void setRemainingMin(Integer remainingMin) { this.remainingMin = remainingMin; }
    public StepStatus getStatus() { return status; }
    public void setStatus(StepStatus status) { this.status = status; }
}
