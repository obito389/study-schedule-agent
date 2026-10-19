package com.studyplanner.dto;

import com.studyplanner.business.Task;
import com.studyplanner.enums.TaskStatus;
import java.time.LocalDateTime;

public class TaskDto {
    private Long id;
    private Long userId;
    private Long courseId;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private String estimateSource;
    private Integer initialEstimateMin;
    private Integer remainingMin;
    private Integer priority;
    private TaskStatus status;
    private String taskType;
    private Long importJobId;
    private LocalDateTime updatedAt;

    public static TaskDto from(Task entity) {
        TaskDto dto = new TaskDto();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setCourseId(entity.getCourseId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setDeadline(entity.getDeadline());
        dto.setEstimateSource(entity.getEstimateSource());
        dto.setInitialEstimateMin(entity.getInitialEstimateMin());
        dto.setRemainingMin(entity.getRemainingMin());
        dto.setPriority(entity.getPriority());
        dto.setStatus(entity.getStatus());
        dto.setTaskType(entity.getTaskType());
        dto.setImportJobId(entity.getImportJobId());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public String getEstimateSource() { return estimateSource; }
    public void setEstimateSource(String estimateSource) { this.estimateSource = estimateSource; }
    public Integer getInitialEstimateMin() { return initialEstimateMin; }
    public void setInitialEstimateMin(Integer initialEstimateMin) { this.initialEstimateMin = initialEstimateMin; }
    public Integer getRemainingMin() { return remainingMin; }
    public void setRemainingMin(Integer remainingMin) { this.remainingMin = remainingMin; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public Long getImportJobId() { return importJobId; }
    public void setImportJobId(Long importJobId) { this.importJobId = importJobId; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
