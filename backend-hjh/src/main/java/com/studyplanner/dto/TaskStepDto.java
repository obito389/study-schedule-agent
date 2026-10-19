package com.studyplanner.dto;

import com.studyplanner.business.TaskStep;
import com.studyplanner.enums.StepStatus;

public class TaskStepDto {
    private Long id;
    private Long taskId;
    private Integer orderNo;
    private String title;
    private Integer remainingMin;
    private StepStatus status;

    public static TaskStepDto from(TaskStep entity) {
        TaskStepDto dto = new TaskStepDto();
        dto.setId(entity.getId());
        dto.setTaskId(entity.getTaskId());
        dto.setOrderNo(entity.getOrderNo());
        dto.setTitle(entity.getTitle());
        dto.setRemainingMin(entity.getRemainingMin());
        dto.setStatus(entity.getStatus());
        return dto;
    }

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
