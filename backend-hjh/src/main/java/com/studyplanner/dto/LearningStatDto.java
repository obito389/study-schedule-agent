package com.studyplanner.dto;

import com.studyplanner.business.LearningStat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LearningStatDto {
    private Long id;
    private Long userId;
    private String taskType;
    private Integer sampleCount;
    private BigDecimal ratio;
    private Long lastEventId;
    private LocalDateTime updatedAt;

    public static LearningStatDto from(LearningStat entity) {
        LearningStatDto dto = new LearningStatDto();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setTaskType(entity.getTaskType());
        dto.setSampleCount(entity.getSampleCount());
        dto.setRatio(entity.getRatio());
        dto.setLastEventId(entity.getLastEventId());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public Integer getSampleCount() { return sampleCount; }
    public void setSampleCount(Integer sampleCount) { this.sampleCount = sampleCount; }
    public BigDecimal getRatio() { return ratio; }
    public void setRatio(BigDecimal ratio) { this.ratio = ratio; }
    public Long getLastEventId() { return lastEventId; }
    public void setLastEventId(Long lastEventId) { this.lastEventId = lastEventId; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
