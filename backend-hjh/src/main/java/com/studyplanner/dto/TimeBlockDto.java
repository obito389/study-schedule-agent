package com.studyplanner.dto;

import com.studyplanner.business.TimeBlock;
import com.studyplanner.enums.BlockStatus;
import java.time.LocalDateTime;

public class TimeBlockDto {
    private Long id;
    private Long userId;
    private Long taskId;
    private Long stepId;
    private Long sourceDraftId;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private BlockStatus status;
    private Boolean locked;

    public static TimeBlockDto from(TimeBlock entity) {
        TimeBlockDto dto = new TimeBlockDto();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setTaskId(entity.getTaskId());
        dto.setStepId(entity.getStepId());
        dto.setSourceDraftId(entity.getSourceDraftId());
        dto.setStartAt(entity.getStartAt());
        dto.setEndAt(entity.getEndAt());
        dto.setStatus(entity.getStatus());
        dto.setLocked(entity.getLocked());
        return dto;
    }

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
