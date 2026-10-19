package com.studyplanner.dto;

import com.studyplanner.business.ExecutionLog;
import java.time.LocalDateTime;

public class ExecutionLogDto {
    private Long id;
    private Long userId;
    private Long taskId;
    private Long blockId;
    private Long stepId;
    private Integer actualMin;
    private Integer remainingAfter;
    private String note;
    private String feedbackKey;
    private LocalDateTime createdAt;

    public static ExecutionLogDto from(ExecutionLog entity) {
        ExecutionLogDto dto = new ExecutionLogDto();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setTaskId(entity.getTaskId());
        dto.setBlockId(entity.getBlockId());
        dto.setStepId(entity.getStepId());
        dto.setActualMin(entity.getActualMin());
        dto.setRemainingAfter(entity.getRemainingAfter());
        dto.setNote(entity.getNote());
        dto.setFeedbackKey(entity.getFeedbackKey());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

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
