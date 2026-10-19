package com.studyplanner.business;

import com.studyplanner.enums.TaskStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * 任务实体（task 表）
 */
@Entity
@Table(name = "task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "import_job_id")
    private Long importJobId;

    @Column(nullable = false, length = 150)
    @NotBlank(message = "任务标题不能为空")
    @Size(max = 150, message = "任务标题不能超过150个字符")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime deadline;

    @Column(nullable = false)
    @Min(value = 0, message = "剩余时间不能为负数")
    private Integer remainingMin = 0;

    @Column(nullable = false)
    @Min(value = 1, message = "优先级最小为1")
    @Max(value = 3, message = "优先级最大为3")
    private Integer priority = 2;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @NotNull(message = "状态不能为空")
    private TaskStatus status = TaskStatus.TODO;

    @Column(name = "task_type", length = 32)
    private String taskType;

    @Column(name = "initial_estimate_min")
    private Integer initialEstimateMin;

    @Column(name = "estimate_source", length = 16)
    private String estimateSource;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Task() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getImportJobId() { return importJobId; }
    public void setImportJobId(Long importJobId) { this.importJobId = importJobId; }
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
    public Integer getInitialEstimateMin() { return initialEstimateMin; }
    public void setInitialEstimateMin(Integer initialEstimateMin) { this.initialEstimateMin = initialEstimateMin; }
    public String getEstimateSource() { return estimateSource; }
    public void setEstimateSource(String estimateSource) { this.estimateSource = estimateSource; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
